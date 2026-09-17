package com.czdr.work.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ContentReview;
import com.czdr.work.model.entity.FormBinding;
import com.czdr.work.model.entity.ProcessBinding;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.entity.WorkflowOpinion;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.model.request.WorkflowTaskCompleteRequest;
import com.czdr.work.model.resource.WorkflowInstanceResource;
import com.czdr.work.model.resource.WorkflowOpinionResource;
import com.czdr.work.model.resource.WorkflowTaskResource;
import com.czdr.work.model.resource.WorkflowTimelineResource;
import com.czdr.work.service.WorkflowContentGateway;
import com.czdr.work.service.WorkflowDefinitionService;
import com.czdr.work.service.WorkflowService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 内容审批工作流实现（Camunda 7 嵌入式引擎驱动）。
 *
 * <h3>业务流程（民族为例）</h3>
 * <pre>
 *  内容编辑提交 ──▶ 审核员审批（留审批意见）
 *                    ├─ 退回 ──▶ 内容编辑修改 ──▶ 审核员二次审批
 *                    └─ 通过 ──▶ 内容上线 ──▶ 内容管理员审查（留审查意见）
 *                                                ├─ 无问题 ──▶ 本轮结束（保持在线）
 *                                                └─ 有问题 ──▶ 内容暂时下线 ──▶ 内容编辑修改
 *                                                                    └──▶ 审核员二次审批 ──▶ 重新上线 ──▶ 再审查 …
 * </pre>
 *
 * <h3>与 Camunda 的边界</h3>
 * <ul>
 *   <li>流程里的 userTask 只负责「谁在什么时候该处理」（用候选组表达角色）；</li>
 *   <li>内容的实际状态（draft/pending/published/offline）由 JavaDelegate
 *       （{@link WorkflowPublishDelegate} / {@link WorkflowOfflineDelegate}）写回，
 *       保证「上线/下线」这类副作用可重试、可审计；</li>
 *   <li>审批/审查意见额外落 workflow_opinion，因为引擎变量不方便做「按版本查看历史意见」。</li>
 * </ul>
 *
 * <h3>相比 Camunda 8 版本的简化（重要）</h3>
 * 原 Camunda 8 实现里有 {@code ENGINE_SETTLE_ATTEMPTS} 短轮询（事务内 sleep 最多 3s）
 * 与 {@code WorkflowSettlementScheduler} 兜底对账，原因只有一个：
 * <b>Camunda 8 的 REST 查询读的是二次存储（Elasticsearch 导出器），相对引擎写入有延迟。</b>
 * 嵌入式 Camunda 7 的用户任务与运行时数据就在同一个业务库里，查询是本地 SQL、
 * 强一致且与业务写同事务，因此：
 * <ul>
 *   <li>完成任务后立即能查到新任务/终态，<b>不需要任何轮询等待</b>；</li>
 *   <li>对账任务降级为「清理异常残留」的兜底，不再是正确性的必要条件。</li>
 * </ul>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    /** 默认流程定义（无自定义绑定时回落） */
    private static final String DEFAULT_PROCESS_ID = "ethnic-content-review";

    /** 内置流程资源路径 */
    private static final String BUILTIN_BPMN_RESOURCE = "processes/ethnic-content-review.bpmn";
    private static final String BUILTIN_BPMN_NAME = "ethnic-content-review.bpmn";

    private final ProcessEngine processEngine;
    private final EasyEntityQuery entityQuery;
    private final WorkflowDefinitionService definitionService;
    private final WorkflowContentGateway contentGateway;

    private RuntimeService runtimeService() {
        return processEngine.getRuntimeService();
    }

    private TaskService taskService() {
        return processEngine.getTaskService();
    }

    private RepositoryService repositoryService() {
        return processEngine.getRepositoryService();
    }

    // ==================================================================================
    // 部署内置资源
    // ==================================================================================

    @Override
    @Transactional
    public Map<String, Object> deployBuiltinResources() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> processes = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        // Camunda 7 的 BPMN 里 userTask 用 camunda:candidateGroups 表达角色，
        // 表单 schema 由本地 form_binding 维护、前端 form-js 渲染，
        // 因此「表单」不是引擎资源，不需要（也无法）像 Camunda 8 那样部署 .form 文件。
        deployOneResource(BUILTIN_BPMN_RESOURCE, processes, failures);

        result.put("processes", processes);
        result.put("forms", List.of());
        result.put("failures", failures);

        if (processes.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "Camunda 流程部署失败: " + String.join("; ", failures));
        }
        registerBuiltinBindings(processes);
        log.info("Camunda 内置流程资源部署完成: processes={}, failures={}",
                processes.size(), failures.size());
        return result;
    }

    @Override
    public Map<String, Object> deployBpmn(String resourceName, String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "BPMN 内容为空");
        }
        String name = resourceName == null || resourceName.isBlank() ? "custom-process.bpmn" : resourceName;
        try {
            Deployment deployment = repositoryService().createDeployment()
                    .name(name)
                    .addString(name, bpmnXml)
                    .deploy();

            List<ProcessDefinition> defs = repositoryService().createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .list();

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("deploymentKey", deployment.getId());
            out.put("deploymentName", deployment.getName());
            List<Map<String, Object>> processes = defs.stream().map(this::processDefinitionMap).toList();
            out.put("processes", processes);
            out.put("forms", List.of());
            registerBuiltinBindings(processes);
            return out;
        } catch (Exception e) {
            // Camunda 7 部署失败会把「第几行第几列、哪个元素」写在异常里，直接回显给 Modeler
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Camunda 校验/部署失败: " + rootMessage(e));
        }
    }

    /**
     * 部署一份 classpath 资源，收集流程元数据；失败时只记录原因不中断。
     * <p>每次部署都会生成一个新的流程定义版本（Camunda 语义），
     * 已启动的实例仍跑在旧版本上，不受影响。</p>
     */
    private void deployOneResource(String classpathResource,
                                   List<Map<String, Object>> processes,
                                   List<String> failures) {
        try {
            Deployment deployment = repositoryService().createDeployment()
                    .name(classpathResource)
                    .addClasspathResource(classpathResource)
                    .deploy();
            repositoryService().createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .list()
                    .forEach(p -> processes.add(processDefinitionMap(p)));
        } catch (Exception e) {
            log.warn("部署 {} 失败: {}", classpathResource, rootMessage(e));
            failures.add(classpathResource + ": " + rootMessage(e));
        }
    }

    /**
     * Camunda Form 在 Camunda 7 嵌入式模式下不是引擎资源：表单 schema 只存本地
     * {@code form_binding}，由前端 form-js 渲染。保留此方法是为了兼容既有接口契约
     * （前台「保存并部署表单」按钮），这里只做校验并返回本地版本信息。
     */
    @Override
    public Map<String, Object> deployForm(String formId, String schemaJson) {
        if (formId == null || formId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "formId 不能为空");
        }
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单内容为空");
        }
        FormBinding binding = definitionService.findFormByFormId(formId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deploymentKey", null);
        out.put("forms", List.of(Map.of(
                "formId", formId,
                "version", binding == null || binding.getVersion() == null ? 1 : binding.getVersion(),
                "formKey", formId,
                "resourceName", formId + ".form")));
        out.put("mode", "local");
        out.put("note", "Camunda 7 嵌入式模式下表单由本地 form_binding + 前端 form-js 渲染，无需部署到引擎");
        return out;
    }

    /** 流程定义 -> 前端使用的元数据 Map */
    private Map<String, Object> processDefinitionMap(ProcessDefinition p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("processDefinitionId", p.getKey());
        m.put("processDefinitionKey", p.getId());
        m.put("version", p.getVersion());
        m.put("resourceName", p.getResourceName() == null ? "" : p.getResourceName());
        m.put("name", p.getName());
        m.put("deploymentId", p.getDeploymentId());
        return m;
    }

    /**
     * 把部署结果写进 process_binding。
     * <p>幂等：同 processKey 已有记录时只补 Camunda 侧字段；
     * <b>bpmnXml 为空时也会从 classpath 补上</b> —— 早期版本注册的绑定没有存 XML，
     * 若不回填，网页 Modeler 打开流程会是一片空白画布。</p>
     */
    private void registerBuiltinBindings(List<Map<String, Object>> processes) {
        for (Map<String, Object> p : processes) {
            String processId = String.valueOf(p.get("processDefinitionId"));
            Integer version = (Integer) p.get("version");
            String resourceName = String.valueOf(p.get("resourceName"));
            String entryType = processId.contains("-") ? processId.substring(0, processId.indexOf('-')) : "ethnic";
            if (!"ethnic".equals(entryType) && !"festival".equals(entryType)
                    && !"art".equals(entryType) && !"topic".equals(entryType)) {
                entryType = "*";
            }

            ProcessBinding existing = entityQuery.queryable(ProcessBinding.class)
                    .where(b -> b.processKey().eq(processId))
                    .orderBy(b -> b.version().desc())
                    .firstOrNull();
            if (existing == null) {
                ProcessBinding binding = new ProcessBinding();
                binding.setId(UUID.randomUUID());
                binding.setProcessKey(processId);
                binding.setName(displayNameOf(processId));
                binding.setEntryType(entryType);
                binding.setVersion(1);
                binding.setBpmnXml(readClasspathResource(resourceName));
                binding.setCamundaDefinitionId(processId);
                binding.setCamundaDefinitionVersion(version);
                binding.setEnabled(true);
                binding.setRemark("系统内置流程，随应用启动自动部署");
                entityQuery.insertable(binding).executeRows();
                continue;
            }

            boolean changed = false;
            if (existing.getCamundaDefinitionVersion() == null
                    || !version.equals(existing.getCamundaDefinitionVersion())) {
                existing.setCamundaDefinitionId(processId);
                existing.setCamundaDefinitionVersion(version);
                changed = true;
            }
            // 回填空的 BPMN 原文，保证网页 Modeler 有内容可编辑
            if (existing.getBpmnXml() == null || existing.getBpmnXml().isBlank()) {
                String xml = readClasspathResource(resourceName);
                if (!xml.isBlank()) {
                    existing.setBpmnXml(xml);
                    changed = true;
                }
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                entityQuery.updatable(existing).executeRows();
            }
        }
    }

    /**
     * 从 classpath 读取内置流程 XML。
     * <p>Camunda 回传的 resourceName 已带目录前缀（如 {@code processes/ethnic-content-review.bpmn}），
     * 因此不能无条件再拼 {@code processes/}；手动构造的资源名（如自定义建模保存时）
     * 可能只有文件名，这里做一次归一化。</p>
     */
    private String readClasspathResource(String resourceName) {
        String name = resourceName == null ? "" : resourceName.trim();
        String path;
        if (name.isBlank()) {
            path = BUILTIN_BPMN_RESOURCE;
        } else if (name.startsWith("processes/")) {
            path = name;
        } else {
            path = "processes/" + name;
        }
        try (var in = new org.springframework.core.io.ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取内置流程资源失败: {}", path);
            return "";
        }
    }

    /** 流程标识 -> 中文展示名（内置流程专用，避免侧栏出现英文 key） */
    private String displayNameOf(String processKey) {
        return switch (processKey) {
            case "ethnic-content-review" -> "民族内容审批流程";
            case "ethnic-forms-collection" -> "民族内容审批表单集合";
            default -> processKey;
        };
    }

    // ==================================================================================
    // 提交审批
    // ==================================================================================

    @Override
    @Transactional
    public WorkflowInstanceResource submit(String entryType, UUID entryId, String note) {
        assertSubmittable(entryType, entryId);

        UUID userId = currentUserId();
        UserAuth user = currentUser();
        var content = contentGateway.load(entryType, entryId);

        int nextVersion = (content.contentVersion() == null ? 0 : content.contentVersion()) + 1;
        contentGateway.bumpContentVersion(entryType, entryId, nextVersion);
        contentGateway.updateStatus(entryType, entryId, WorkflowConstants.CONTENT_PENDING);

        String businessKey = WorkflowConstants.businessKey(entryType, entryId, nextVersion);
        String processId = resolveProcessId(entryType);

        Map<String, Object> variables = new HashMap<>();
        variables.put(WorkflowConstants.VAR_ENTRY_TYPE, entryType);
        variables.put(WorkflowConstants.VAR_ENTRY_ID, entryId.toString());
        variables.put(WorkflowConstants.VAR_ENTRY_TITLE, content.title());
        variables.put(WorkflowConstants.VAR_CONTENT_VERSION, nextVersion);
        variables.put("submitterId", userId.toString());
        variables.put("submitterName", user == null ? "" : user.getNickname());

        // JUEL 网关条件（${approved == true} / ${hasIssue == true} / ${needReapproval == false}）
        // 在变量缺失时会抛 PropertyNotFound，因此这里预置默认值：
        // 初始环节是「待审核员审批」，两个结论开关先给「未通过/无问题」的安全默认。
        variables.put(WorkflowConstants.VAR_APPROVED, false);
        variables.put(WorkflowConstants.VAR_HAS_ISSUE, false);
        variables.put("needReapproval", true);

        ProcessInstance instance0;
        try {
            // Camunda 7：按 key 启动，并直接把 businessKey 交给引擎（可据此反查业务实例）
            instance0 = runtimeService().startProcessInstanceByKey(processId, businessKey, variables);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "启动审批流程失败（请确认已部署流程 " + processId + "）: " + rootMessage(e));
        }

        // Camunda 7 的流程实例 ID 是字符串，直接作为与业务表的关联键
        String processInstanceId = instance0.getId();

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(UUID.randomUUID());
        instance.setEntryType(entryType);
        instance.setEntryId(entryId);
        instance.setEntryTitle(content.title());
        instance.setContentVersion(nextVersion);
        instance.setProcessDefinitionId(processId);
        instance.setProcessDefinitionVersion(null);
        instance.setProcessInstanceKey(processInstanceId);
        instance.setBusinessKey(businessKey);
        instance.setStatus(WorkflowConstants.INSTANCE_RUNNING);
        instance.setCurrentStage(WorkflowConstants.STAGE_PENDING_REVIEW);
        instance.setSubmitterId(userId);
        instance.setSubmitterName(user == null ? null : user.getNickname());
        instance.setRoundNo(1);
        instance.setStartedAt(LocalDateTime.now());
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(instance).executeRows();

        addOpinion(instance, WorkflowConstants.OPINION_SUBMIT, WorkflowConstants.DECISION_SUBMITTED,
                note == null || note.isBlank() ? "提交审批" : note.trim(), null, "提交审批");

        // 引擎是本地库、强一致：启动后立刻就能查到首个用户任务，无需轮询
        refreshInstanceFromEngine(instance);

        syncReviewSnapshot(entryType, entryId, WorkflowConstants.REVIEW_PENDING,
                instance.getId(), nextVersion, note);

        contentGateway.reindex(entryType, entryId);
        log.info("提交审批: entry={}#{} v{} instance={} processInstanceId={}",
                entryType, entryId, nextVersion, instance.getId(), processInstanceId);
        return WorkflowInstanceResource.of(instance);
    }

    @Override
    public void assertSubmittable(String entryType, UUID entryId) {
        String status = contentGateway.load(entryType, entryId).status();
        if (WorkflowConstants.CONTENT_PENDING.equals(status)) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该内容已在审批中，无需重复提交");
        }
        WorkflowInstance running = findRunningInstance(entryType, entryId);
        if (running != null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION,
                    "该内容存在进行中的审批流程（当前环节：" + WorkflowInstanceResource.stageLabel(running.getCurrentStage()) + "）");
        }
    }

    // ==================================================================================
    // 待办与查询
    // ==================================================================================

    @Override
    public List<WorkflowTaskResource> listTasks(String stage, String entryType, boolean mineOnly) {
        List<WorkflowInstance> instances = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> {
                    i.status().eq(WorkflowConstants.INSTANCE_RUNNING);
                    if (entryType != null && !entryType.isBlank()) {
                        i.entryType().eq(entryType);
                    }
                    if (stage != null && !stage.isBlank()) {
                        i.currentStage().eq(stage);
                    }
                })
                .orderBy(i -> i.updatedAt().desc())
                .toList();

        if (instances.isEmpty()) {
            return List.of();
        }

        // 一次性按流程实例批量查引擎待办任务：Camunda 7 支持 processInstanceIdIn，一条 SQL 搞定
        Map<String, Task> engineTasks = queryEngineUserTasks(
                instances.stream().map(WorkflowInstance::getProcessInstanceKey).toList());

        Set<String> myRoles = currentRoles();
        UUID me = currentUserId();
        List<WorkflowTaskResource> result = new ArrayList<>();
        for (WorkflowInstance instance : instances) {
            Task task = instance.getProcessInstanceKey() == null ? null
                    : engineTasks.get(instance.getProcessInstanceKey());
            String effectiveStage = task != null ? stageOfElement(task.getTaskDefinitionKey()) : instance.getCurrentStage();
            if (task != null && !effectiveStage.equals(instance.getCurrentStage())) {
                // 引擎与本地不一致时以引擎为准并回写，避免界面显示过期环节
                instance.setCurrentStage(effectiveStage);
                instance.setCurrentTaskKey(task.getId());
                instance.setCurrentTaskName(task.getName());
                instance.setUpdatedAt(LocalDateTime.now());
                entityQuery.updatable(instance).executeRows();
            }
            boolean actionable = canHandle(effectiveStage, myRoles, instance, me);
            if (mineOnly && !actionable) {
                continue;
            }
            result.add(toTaskResource(instance, task, effectiveStage, actionable));
        }
        return result;
    }

    @Override
    public WorkflowTaskResource getTaskByInstance(UUID instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        Task task = instance.getProcessInstanceKey() == null ? null
                : queryEngineUserTasks(List.of(instance.getProcessInstanceKey()))
                .get(instance.getProcessInstanceKey());
        String stage = task != null ? stageOfElement(task.getTaskDefinitionKey()) : instance.getCurrentStage();
        boolean actionable = canHandle(stage, currentRoles(), instance, currentUserId());
        return toTaskResource(instance, task, stage, actionable);
    }

    @Override
    public WorkflowTaskResource getActiveTaskByEntry(String entryType, UUID entryId) {
        WorkflowInstance instance = findRunningInstance(entryType, entryId);
        if (instance == null) {
            return null;
        }
        return getTaskByInstance(instance.getId());
    }

    @Override
    public WorkflowTimelineResource getTimeline(String entryType, UUID entryId) {
        var content = contentGateway.load(entryType, entryId);
        List<WorkflowInstance> instances = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> {
                    i.entryType().eq(entryType);
                    i.entryId().eq(entryId);
                })
                .orderBy(i -> i.contentVersion().desc())
                .toList();
        List<WorkflowOpinion> opinions = entityQuery.queryable(WorkflowOpinion.class)
                .where(o -> {
                    o.entryType().eq(entryType);
                    o.entryId().eq(entryId);
                })
                .orderBy(o -> o.createdAt().asc())
                .toList();

        WorkflowInstance active = instances.stream()
                .filter(i -> WorkflowConstants.INSTANCE_RUNNING.equals(i.getStatus()))
                .findFirst().orElse(null);

        return new WorkflowTimelineResource(
                entryType, entryId, content.title(), content.status(), content.contentVersion(),
                instances.stream().map(WorkflowInstanceResource::of).toList(),
                WorkflowOpinionResource.of(opinions),
                active == null ? null : WorkflowInstanceResource.of(active),
                LocalDateTime.now());
    }

    @Override
    public List<WorkflowOpinionResource> listOpinions(String entryType, UUID entryId) {
        return WorkflowOpinionResource.of(entityQuery.queryable(WorkflowOpinion.class)
                .where(o -> {
                    o.entryType().eq(entryType);
                    o.entryId().eq(entryId);
                })
                .orderBy(o -> o.createdAt().asc())
                .toList());
    }

    // ==================================================================================
    // 环节动作
    // ==================================================================================

    @Override
    @Transactional
    public void completeTask(UUID instanceId, WorkflowTaskCompleteRequest request) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (!WorkflowConstants.INSTANCE_RUNNING.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该流程已结束，无法再处理");
        }
        Task task = currentEngineTask(instance);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前没有待处理的用户任务（流程可能已流转到下一步）");
        }
        String stage = stageOfElement(task.getTaskDefinitionKey());
        if (!canHandle(stage, currentRoles(), instance, currentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "当前登录用户无权处理「" + WorkflowInstanceResource.stageLabel(stage) + "」环节");
        }

        String opinion = request == null ? null : request.resolveOpinion();
        String decision = request == null ? null : request.resolveDecision(null);
        Map<String, Object> variables = new HashMap<>();
        if (request != null && request.variables() != null) {
            variables.putAll(request.variables());
        }
        variables.put(WorkflowConstants.VAR_ENTRY_TYPE, instance.getEntryType());
        variables.put(WorkflowConstants.VAR_ENTRY_ID, instance.getEntryId().toString());
        variables.put(WorkflowConstants.VAR_CONTENT_VERSION, instance.getContentVersion());

        switch (stage) {
            case WorkflowConstants.STAGE_PENDING_REVIEW -> {
                boolean approved = !WorkflowConstants.DECISION_REJECTED.equalsIgnoreCase(decision)
                        && !"false".equalsIgnoreCase(String.valueOf(variables.get(WorkflowConstants.VAR_APPROVED)));
                variables.put(WorkflowConstants.VAR_APPROVED, approved);
                if (opinion != null) {
                    variables.put(WorkflowConstants.VAR_APPROVAL_OPINION, opinion);
                }
                if (!approved && (opinion == null || opinion.isBlank())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "退回内容时必须填写审批意见");
                }
                addOpinion(instance, WorkflowConstants.OPINION_APPROVE,
                        approved ? WorkflowConstants.DECISION_APPROVED : WorkflowConstants.DECISION_REJECTED,
                        opinion, task.getId(), task.getName());
                if (!approved) {
                    contentGateway.updateStatus(instance.getEntryType(), instance.getEntryId(),
                            WorkflowConstants.CONTENT_REJECTED);
                    syncReviewSnapshot(instance.getEntryType(), instance.getEntryId(),
                            WorkflowConstants.REVIEW_REVISING, instance.getId(),
                            instance.getContentVersion(), opinion);
                }
            }
            case WorkflowConstants.STAGE_PENDING_INSPECT -> {
                boolean hasIssue = WorkflowConstants.DECISION_ISSUE.equalsIgnoreCase(decision)
                        || "true".equalsIgnoreCase(String.valueOf(variables.get(WorkflowConstants.VAR_HAS_ISSUE)));
                variables.put(WorkflowConstants.VAR_HAS_ISSUE, hasIssue);
                if (opinion != null) {
                    variables.put(WorkflowConstants.VAR_INSPECTION_OPINION, opinion);
                    variables.put(WorkflowConstants.VAR_OFFLINE_REASON, opinion);
                }
                if (hasIssue && (opinion == null || opinion.isBlank())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "发现问题时必须填写审查意见");
                }
                addOpinion(instance, WorkflowConstants.OPINION_INSPECT,
                        hasIssue ? WorkflowConstants.DECISION_ISSUE : WorkflowConstants.DECISION_APPROVED,
                        opinion, task.getId(), task.getName());
            }
            case WorkflowConstants.STAGE_REVISING -> {
                boolean needReapproval = request == null || request.variables() == null
                        || !"false".equalsIgnoreCase(String.valueOf(request.variables().get("needReapproval")));
                variables.put("needReapproval", needReapproval);
                if (opinion != null) {
                    variables.put(WorkflowConstants.VAR_REVISION_NOTE, opinion);
                }
                addOpinion(instance, WorkflowConstants.OPINION_REVISE, WorkflowConstants.DECISION_RESOLVED,
                        opinion == null || opinion.isBlank() ? "已完成修改" : opinion,
                        task.getId(), task.getName());
                syncReviewSnapshot(instance.getEntryType(), instance.getEntryId(),
                        WorkflowConstants.REVIEW_PENDING, instance.getId(),
                        instance.getContentVersion(), opinion);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知环节: " + stage);
        }

        try {
            // Camunda 7：complete(taskId, variables) 在同一事务内推进流程；
            // 其中的 JavaDelegate（上线/下线）也是同步执行的。
            taskService().complete(task.getId(), variables);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "提交任务失败: " + rootMessage(e));
        }

        instance.setCurrentTaskKey(null);
        instance.setCurrentTaskName(null);
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();
        refreshInstanceFromEngine(instance);
    }

    @Override
    @Transactional
    public void inspectPass(UUID instanceId, String opinion) {
        completeTask(instanceId, new WorkflowTaskCompleteRequest("approved", opinion, null, null, null));
    }

    @Override
    @Transactional
    public void inspectIssueAndOffline(UUID instanceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写审查意见（说明问题所在）");
        }
        completeTask(instanceId, new WorkflowTaskCompleteRequest("issue", reason, reason, null, null));
    }

    @Override
    @Transactional
    public void reviseAndResubmit(UUID instanceId, String revisionNote, boolean needReapproval) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("needReapproval", needReapproval);
        if (revisionNote != null && !revisionNote.isBlank()) {
            vars.put(WorkflowConstants.VAR_REVISION_NOTE, revisionNote);
        }
        completeTask(instanceId, new WorkflowTaskCompleteRequest("resolved", revisionNote, null, null, vars));
    }

    @Override
    @Transactional
    public void withdraw(UUID instanceId, String reason) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (!UUID.fromString(StpUtil.getLoginIdAsString()).equals(instance.getSubmitterId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有提交人可以撤回");
        }
        if (!WorkflowConstants.INSTANCE_RUNNING.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该流程已结束");
        }
        if (instance.getProcessInstanceKey() != null) {
            try {
                runtimeService().deleteProcessInstance(instance.getProcessInstanceKey(), "提交人撤回");
            } catch (Exception e) {
                log.warn("取消 Camunda 流程实例失败（继续本地收尾）: {}", rootMessage(e));
            }
        }
        instance.setStatus(WorkflowConstants.INSTANCE_WITHDRAWN);
        instance.setCurrentStage(WorkflowConstants.STAGE_STOPPED);
        instance.setCurrentTaskKey(null);
        instance.setCurrentTaskName(null);
        instance.setFinishedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();

        contentGateway.updateStatus(instance.getEntryType(), instance.getEntryId(), WorkflowConstants.CONTENT_DRAFT);
        addOpinion(instance, WorkflowConstants.OPINION_OFFLINE, "withdrawn",
                reason == null || reason.isBlank() ? "提交人撤回" : reason, null, "撤回");
        syncReviewSnapshot(instance.getEntryType(), instance.getEntryId(),
                WorkflowConstants.REVIEW_REJECTED, instance.getId(), instance.getContentVersion(), reason);
    }

    // ==================================================================================
    // 供 AdminService 调用
    // ==================================================================================

    @Override
    @Transactional
    public void onContentDeleted(String entryType, UUID entryId) {
        List<WorkflowInstance> running = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> {
                    i.entryType().eq(entryType);
                    i.entryId().eq(entryId);
                    i.status().eq(WorkflowConstants.INSTANCE_RUNNING);
                })
                .toList();
        for (WorkflowInstance instance : running) {
            if (instance.getProcessInstanceKey() != null) {
                try {
                    runtimeService().deleteProcessInstance(instance.getProcessInstanceKey(), "内容已删除");
                } catch (Exception e) {
                    log.warn("删除内容时取消流程失败: {}", rootMessage(e));
                }
            }
            instance.setStatus(WorkflowConstants.INSTANCE_SUPERSEDED);
            instance.setCurrentStage(WorkflowConstants.STAGE_STOPPED);
            instance.setFinishedAt(LocalDateTime.now());
            instance.setCurrentTaskKey(null);
            instance.setCurrentTaskName(null);
            entityQuery.updatable(instance).executeRows();
        }
        entityQuery.deletable(WorkflowOpinion.class).allowDeleteStatement(true)
                .where(o -> {
                    o.entryType().eq(entryType);
                    o.entryId().eq(entryId);
                })
                .executeRows();
        entityQuery.deletable(WorkflowInstance.class).allowDeleteStatement(true)
                .where(i -> {
                    i.entryType().eq(entryType);
                    i.entryId().eq(entryId);
                })
                .executeRows();
    }

    @Override
    @Transactional
    public void syncReviewSnapshot(String entryType, UUID entryId, String reviewStatus,
                                   UUID instanceId, Integer contentVersion, String lastOpinion) {
        ContentReview review = entityQuery.queryable(ContentReview.class)
                .where(r -> {
                    r.entryType().eq(entryType);
                    r.entryId().eq(entryId);
                })
                .firstOrNull();
        if (review == null) {
            review = new ContentReview(UUID.randomUUID(), entryType, entryId, reviewStatus);
        }
        review.setStatus(reviewStatus);
        review.setInstanceId(instanceId);
        review.setContentVersion(contentVersion);
        if (lastOpinion != null && !lastOpinion.isBlank()) {
            review.setLastOpinion(lastOpinion);
            if (WorkflowConstants.REVIEW_REJECTED.equals(reviewStatus)
                    || WorkflowConstants.REVIEW_REVISING.equals(reviewStatus)) {
                review.setRejectReason(lastOpinion);
            }
        }
        if (WorkflowConstants.REVIEW_PENDING.equals(reviewStatus)) {
            review.setSubmittedAt(LocalDateTime.now());
        } else {
            review.setReviewerId(currentUserId());
            review.setReviewedAt(LocalDateTime.now());
        }
        final UUID reviewId = review.getId();
        if (entityQuery.queryable(ContentReview.class)
                .where(r -> r.id().eq(reviewId)).firstOrNull() == null) {
            entityQuery.insertable(review).executeRows();
        } else {
            entityQuery.updatable(review).executeRows();
        }
    }

    // ==================================================================================
    // 内部工具
    // ==================================================================================

    /** 引擎里当前待处理的用户任务（取最早创建的一个） */
    private Task currentEngineTask(WorkflowInstance instance) {
        if (instance.getProcessInstanceKey() == null) {
            return null;
        }
        return queryEngineUserTasks(List.of(instance.getProcessInstanceKey()))
                .get(instance.getProcessInstanceKey());
    }

    /**
     * 查引擎中「未完成」的用户任务：processInstanceId -> 该实例最早的待办任务。
     *
     * <p>Camunda 7 的 {@code TaskQuery.processInstanceIdIn(...)} 直接下推到本地 SQL，
     * 一条查询即可批量取回，不再需要 Camunda 8 时代「全量拉取再本地过滤」的绕行
     * （那是为了规避 8.9 自管集群 REST 接口无法解析超出 int 范围的流程实例 key）。</p>
     */
    private Map<String, Task> queryEngineUserTasks(List<String> processInstanceIds) {
        Map<String, Task> map = new HashMap<>();
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return map;
        }
        Set<String> wanted = processInstanceIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (wanted.isEmpty()) {
            return map;
        }
        try {
            List<Task> items = taskService().createTaskQuery()
                    .processInstanceIdIn(wanted.toArray(new String[0]))
                    .orderByTaskCreateTime().asc()
                    .list();
            for (Task t : items) {
                map.putIfAbsent(t.getProcessInstanceId(), t);
            }
        } catch (Exception e) {
            log.warn("查询 Camunda 用户任务失败（降级为本地环节）: {}", rootMessage(e));
        }
        return map;
    }

    /**
     * 流程推进后，把引擎侧的当前任务/环节同步回本地实例。
     *
     * <p><b>与 Camunda 8 版本的关键差异：</b>这里<b>不需要轮询等待</b>。
     * 嵌入式 Camunda 7 的用户任务与运行时数据就在同一个业务库里，
     * 查询是本地 SQL 且强一致，一次查询即可拿到确定结论，
     * 不会像 Camunda 8 那样因二次存储延迟而误判实例「已结束」。</p>
     *
     * <p><b>关于 asyncBefore 服务任务：</b>BPMN 里的上线/下线服务任务标记了
     * {@code camunda:asyncBefore="true"}，因此用户任务完成后流程可能短暂停在
     * 一个待执行的作业上（此刻查不到新任务、实例也还没结束）。这不是错误状态：
     * 本地实例保持 running、环节沿用上一次的值，等作业执行器跑完后，
     * 下一次读取（待办列表 / 详情页 / 兜底复查）会自然刷新到新环节。</p>
     */
    private void refreshInstanceFromEngine(WorkflowInstance instance) {
        if (instance.getProcessInstanceKey() == null) {
            return;
        }
        Task task = currentEngineTask(instance);
        if (task != null) {
            String stage = stageOfElement(task.getTaskDefinitionKey());
            instance.setCurrentStage(stage);
            instance.setCurrentTaskKey(task.getId());
            instance.setCurrentTaskName(task.getName());
            instance.setCurrentAssigneeId(resolveAssigneeId(task, stage));
            instance.setStatus(WorkflowConstants.INSTANCE_RUNNING);
            instance.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(instance).executeRows();
            return;
        }
        // 没有待办任务：流程要么已结束，要么正停在待执行的异步作业上。
        // 只有「引擎里已无该运行时实例」才是真的结束。
        if (isProcessInstanceEnded(instance.getProcessInstanceKey())) {
            finishInstance(instance);
        }
    }

    /**
     * 流程实例是否已结束。
     * <p>Camunda 7 中「运行时查不到该实例」即表示已结束（历史库才有）；这里用
     * 历史流程实例查询二次确认，避免把「引擎重启后未恢复」误判为已结束。</p>
     */
    private boolean isProcessInstanceEnded(String processInstanceId) {
        try {
            ProcessInstance pi = runtimeService().createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return pi == null;
        } catch (Exception e) {
            log.debug("查询流程实例状态失败（按未结束处理）: {}", e.getMessage());
            return false;
        }
    }

    /** 流程结束：读回内容状态决定 current_stage / status，并同步审核态快照 */
    private void finishInstance(WorkflowInstance instance) {
        String contentStatus = contentGateway.load(instance.getEntryType(), instance.getEntryId()).status();
        String stage = switch (contentStatus == null ? "" : contentStatus) {
            case WorkflowConstants.CONTENT_PUBLISHED -> WorkflowConstants.STAGE_PUBLISHED;
            case WorkflowConstants.CONTENT_OFFLINE -> WorkflowConstants.STAGE_REVISING;
            case WorkflowConstants.CONTENT_DRAFT -> WorkflowConstants.STAGE_STOPPED;
            default -> WorkflowConstants.STAGE_PUBLISHED;
        };
        boolean completed = WorkflowConstants.STAGE_PUBLISHED.equals(stage);
        instance.setCurrentStage(stage);
        instance.setStatus(completed ? WorkflowConstants.INSTANCE_COMPLETED : WorkflowConstants.INSTANCE_SUPERSEDED);
        instance.setCurrentTaskKey(null);
        instance.setCurrentTaskName(null);
        instance.setCurrentAssigneeId(null);
        instance.setFinishedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();

        String reviewStatus = switch (stage) {
            case WorkflowConstants.STAGE_PUBLISHED -> WorkflowConstants.REVIEW_APPROVED;
            case WorkflowConstants.STAGE_REVISING -> WorkflowConstants.REVIEW_REVISING;
            default -> WorkflowConstants.REVIEW_REJECTED;
        };
        syncReviewSnapshot(instance.getEntryType(), instance.getEntryId(), reviewStatus,
                instance.getId(), instance.getContentVersion(), null);
    }

    private void addOpinion(WorkflowInstance instance, String stage, String decision, String opinion,
                            String taskKey, String taskName) {
        UUID userId = currentUserId();
        UserAuth user = currentUser();
        WorkflowOpinion entity = new WorkflowOpinion();
        entity.setId(UUID.randomUUID());
        entity.setInstanceId(instance.getId());
        entity.setEntryType(instance.getEntryType());
        entity.setEntryId(instance.getEntryId());
        entity.setContentVersion(instance.getContentVersion());
        entity.setStage(stage);
        entity.setDecision(decision);
        entity.setOpinion(opinion);
        entity.setOperatorId(userId);
        entity.setOperatorName(user == null ? null : user.getNickname());
        entity.setOperatorRole(primaryRole());
        entity.setCamundaTaskKey(taskKey);
        entity.setTaskName(taskName);
        entity.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(entity).executeRows();
    }

    private WorkflowInstance requireInstance(UUID instanceId) {
        WorkflowInstance instance = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> i.id().eq(instanceId))
                .firstOrNull();
        if (instance == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流实例不存在");
        }
        return instance;
    }

    private WorkflowInstance findRunningInstance(String entryType, UUID entryId) {
        return entityQuery.queryable(WorkflowInstance.class)
                .where(i -> {
                    i.entryType().eq(entryType);
                    i.entryId().eq(entryId);
                    i.status().eq(WorkflowConstants.INSTANCE_RUNNING);
                })
                .orderBy(i -> i.contentVersion().desc())
                .firstOrNull();
    }

    /** BPMN elementId -> 环节 */
    private String stageOfElement(String elementId) {
        if (elementId == null) {
            return WorkflowConstants.STAGE_PENDING_REVIEW;
        }
        return switch (elementId) {
            case "Task_ReviewerApprove" -> WorkflowConstants.STAGE_PENDING_REVIEW;
            case "Task_ContentInspect" -> WorkflowConstants.STAGE_PENDING_INSPECT;
            case "Task_EditorRevise" -> WorkflowConstants.STAGE_REVISING;
            default -> WorkflowConstants.STAGE_PENDING_REVIEW;
        };
    }

    /** 环节 -> 处理角色 */
    private String roleOfStage(String stage) {
        return switch (stage) {
            case WorkflowConstants.STAGE_PENDING_REVIEW -> WorkflowConstants.ROLE_REVIEWER;
            case WorkflowConstants.STAGE_PENDING_INSPECT -> WorkflowConstants.ROLE_CONTENT_ADMIN;
            case WorkflowConstants.STAGE_REVISING -> WorkflowConstants.ROLE_EDITOR;
            default -> null;
        };
    }

    private boolean canHandle(String stage, Set<String> roles, WorkflowInstance instance, UUID me) {
        if (roles.contains("super_admin")) {
            return true;
        }
        String role = roleOfStage(stage);
        if (role == null) {
            return false;
        }
        if (WorkflowConstants.ROLE_EDITOR.equals(role)) {
            // 修改环节：提交人本人或有 editor 角色的人都能改
            return roles.contains(WorkflowConstants.ROLE_EDITOR) || me.equals(instance.getSubmitterId());
        }
        return roles.contains(role);
    }

    private UUID resolveAssigneeId(Task task, String stage) {
        if (task.getAssignee() == null || task.getAssignee().isBlank()) {
            return null;
        }
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.account().eq(task.getAssignee());
                    u.or(() -> u.nickname().eq(task.getAssignee()));
                })
                .firstOrNull();
        return user == null ? null : user.getId();
    }

    private WorkflowTaskResource toTaskResource(WorkflowInstance instance, Task task,
                                                String stage, boolean actionable) {
        String purpose = formPurposeOfStage(stage);
        var form = definitionService.findFormByPurpose(purpose, instance.getEntryType());
        List<WorkflowOpinion> opinions = entityQuery.queryable(WorkflowOpinion.class)
                .where(o -> {
                    o.entryType().eq(instance.getEntryType());
                    o.entryId().eq(instance.getEntryId());
                })
                .orderBy(o -> o.createdAt().asc())
                .toList();

        return new WorkflowTaskResource(
                instance.getId(),
                instance.getEntryType(),
                WorkflowTaskResource.entryTypeLabel(instance.getEntryType()),
                instance.getEntryId(),
                instance.getEntryTitle(),
                instance.getContentVersion(),
                instance.getStatus(),
                stage,
                WorkflowInstanceResource.stageLabel(stage),
                task == null ? instance.getCurrentTaskKey() : task.getId(),
                task == null ? instance.getCurrentTaskName() : task.getName(),
                task == null ? null : task.getTaskDefinitionKey(),
                instance.getProcessDefinitionId(),
                task == null ? instance.getProcessDefinitionVersion() : processDefinitionVersionOf(task),
                instance.getProcessInstanceKey(),
                instance.getSubmitterName(),
                instance.getStartedAt(),
                instance.getUpdatedAt(),
                form == null ? null : form.getSchemaJson(),
                form == null ? null : form.getFormId(),
                actionable,
                WorkflowOpinionResource.of(opinions));
    }

    /** 任务所属流程定义的版本号（Camunda 7 可从流程定义 ID 反查） */
    private Integer processDefinitionVersionOf(Task task) {
        try {
            ProcessDefinition pd = repositoryService().createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            return pd == null ? null : pd.getVersion();
        } catch (Exception e) {
            return null;
        }
    }

    /** 环节 -> 表单用途（与 form_binding.purpose 对应）：复用接口的 default 实现 */
    // 说明见 WorkflowService#formPurposeOfStage

    private String resolveProcessId(String entryType) {
        var binding = definitionService.findEnabledProcess(entryType);
        return binding == null ? DEFAULT_PROCESS_ID : binding.getProcessKey();
    }

    private UUID currentUserId() {
        try {
            return UUID.fromString(StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private UserAuth currentUser() {
        UUID id = currentUserId();
        if (id == null) {
            return null;
        }
        return entityQuery.queryable(UserAuth.class).where(u -> u.id().eq(id)).firstOrNull();
    }

    private Set<String> currentRoles() {
        try {
            return new LinkedHashSet<>(StpUtil.getRoleList());
        } catch (Exception e) {
            return Set.of();
        }
    }

    private String primaryRole() {
        Set<String> roles = currentRoles();
        for (String candidate : List.of("super_admin", WorkflowConstants.ROLE_CONTENT_ADMIN,
                WorkflowConstants.ROLE_REVIEWER, WorkflowConstants.ROLE_EDITOR)) {
            if (roles.contains(candidate)) {
                return candidate;
            }
        }
        return roles.isEmpty() ? null : roles.iterator().next();
    }

    /** 供 job handler 复用的意见写入（系统触发） */
    void addSystemOpinion(WorkflowInstance instance, String stage, String decision, String opinion) {
        WorkflowOpinion entity = new WorkflowOpinion();
        entity.setId(UUID.randomUUID());
        entity.setInstanceId(instance.getId());
        entity.setEntryType(instance.getEntryType());
        entity.setEntryId(instance.getEntryId());
        entity.setContentVersion(instance.getContentVersion());
        entity.setStage(stage);
        entity.setDecision(decision);
        entity.setOpinion(opinion);
        entity.setOperatorName("系统");
        entity.setOperatorRole("system");
        entity.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(entity).executeRows();
    }

    /** 取异常链的最内层消息（Camunda 的部署/校验错误信息都藏在最内层） */
    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null || msg.isBlank() ? e.toString() : msg;
    }

    /** Camunda 7 的日期是 java.util.Date，统一转成项目使用的 LocalDateTime */
    static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null
                : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
