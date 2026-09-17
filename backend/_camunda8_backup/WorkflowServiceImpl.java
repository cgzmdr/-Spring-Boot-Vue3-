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
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 内容审批工作流实现（Camunda 8 用户任务驱动）。
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
 *   <li>流程里的 userTask 只负责「谁在什么时候该处理」；</li>
 *   <li>内容的实际状态（draft/pending/published/offline）由本地 serviceTask 处理类
 *       {@link WorkflowJobHandlers} 写回，保证「上线/下线」这类副作用可重试、可审计；</li>
 *   <li>审批/审查意见额外落 workflow_opinion，因为引擎变量不方便做「按版本查看历史意见」。</li>
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

    /** 用户任务完成后等待引擎流转的轮询次数与间隔（服务任务异步执行，需要短暂等待） */
    private static final int ENGINE_SETTLE_ATTEMPTS = 12;
    private static final long ENGINE_SETTLE_INTERVAL_MS = 250L;

    private final CamundaClient camundaClient;
    private final EasyEntityQuery entityQuery;
    private final WorkflowDefinitionService definitionService;
    private final WorkflowContentGateway contentGateway;

    // ==================================================================================
    // 部署内置资源
    // ==================================================================================

    @Override
    @Transactional
    public Map<String, Object> deployBuiltinResources() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> processes = new ArrayList<>();
        List<Map<String, Object>> forms = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        // 表单必须先于流程部署：Camunda 的 userTask 一旦引用了不存在的 formId，
        // 任务创建时会直接抛 FORM_NOT_FOUND incident（流程卡住），所以顺序不能反。
        deployBuiltinForms(forms, failures);

        // 逐份部署而不是一次带两个资源：Camunda 对「其中一份不合法」是整批拒绝，
        // 分开部署能让失败原因更聚焦。
        deployOneResource("processes/ethnic-content-review.bpmn", processes, failures);

        result.put("processes", processes);
        result.put("forms", forms);
        result.put("failures", failures);

        if (processes.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "Camunda 流程部署失败: " + String.join("; ", failures));
        }
        // 回填/注册流程绑定
        registerBuiltinBindings(processes);
        log.info("Camunda 内置流程资源部署完成: processes={}, forms={}, failures={}",
                processes.size(), forms.size(), failures.size());
        return result;
    }

    /**
     * 把 form_binding 中启用中的 Camunda Form 部署到引擎。
     * <p>Camunda 8 的表单是独立资源（{@code .form} 文件），流程里只按 formId 引用；
     * 因此表单一改动就需要重新部署，否则引擎侧仍是旧版本。</p>
     */
    private void deployBuiltinForms(List<Map<String, Object>> forms, List<String> failures) {
        List<FormBinding> bindings = entityQuery.queryable(FormBinding.class)
                .where(b -> b.enabled().eq(true))
                .toList();
        for (FormBinding binding : bindings) {
            try {
                DeploymentEvent event = camundaClient.newDeployResourceCommand()
                        .addResourceStream(new ByteArrayInputStream(
                                        binding.getSchemaJson().getBytes(StandardCharsets.UTF_8)),
                                binding.getFormId() + ".form")
                        .send()
                        .join();
                if (event.getForm() != null) {
                    event.getForm().forEach(f -> forms.add(new LinkedHashMap<>(Map.of(
                            "formId", f.getFormId(),
                            "version", f.getVersion(),
                            "formKey", f.getFormKey(),
                            "resourceName", f.getResourceName() == null ? "" : f.getResourceName()))));
                }
            } catch (Exception e) {
                log.warn("部署 Camunda Form {} 失败: {}", binding.getFormId(), e.getMessage());
                failures.add("form:" + binding.getFormId() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> deployBpmn(String resourceName, String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "BPMN 内容为空");
        }
        String name = resourceName == null || resourceName.isBlank() ? "custom-process.bpmn" : resourceName;
        try {
            DeploymentEvent event = camundaClient.newDeployResourceCommand()
                    .addResourceStream(new ByteArrayInputStream(
                            bpmnXml.getBytes(StandardCharsets.UTF_8)), name)
                    .send()
                    .join();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("deploymentKey", event.getKey());
            List<Map<String, Object>> processes = event.getProcesses().stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("processDefinitionId", p.getBpmnProcessId());
                m.put("processDefinitionKey", p.getProcessDefinitionKey());
                m.put("version", p.getVersion());
                m.put("resourceName", p.getResourceName());
                return m;
            }).toList();
            out.put("processes", processes);
            out.put("forms", event.getForm() == null ? List.of() : event.getForm().stream().map(f -> Map.of(
                    "formId", f.getFormId(),
                    "version", f.getVersion(),
                    "formKey", f.getFormKey())).toList());
            registerBuiltinBindings(processes);
            return out;
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Camunda 校验/部署失败: " + e.getMessage());
        }
    }

    /** 部署一份 classpath 资源，收集流程元数据；失败时只记录原因不中断 */
    private void deployOneResource(String classpathResource,
                                   List<Map<String, Object>> processes,
                                   List<String> failures) {
        try {
            DeploymentEvent event = camundaClient.newDeployResourceCommand()
                    .addResourceFromClasspath(classpathResource)
                    .send()
                    .join();
            event.getProcesses().forEach(p -> processes.add(new LinkedHashMap<>(Map.of(
                    "processDefinitionId", p.getBpmnProcessId(),
                    "processDefinitionKey", p.getProcessDefinitionKey(),
                    "version", p.getVersion(),
                    "resourceName", p.getResourceName() == null ? "" : p.getResourceName()))));
        } catch (Exception e) {
            log.warn("部署 {} 失败: {}", classpathResource, e.getMessage());
            failures.add(classpathResource + ": " + e.getMessage());
        }
    }

    /** 单独部署一份表单（供 Modeler 保存后立即生效） */
    @Override
    public Map<String, Object> deployForm(String formId, String schemaJson) {
        if (formId == null || formId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "formId 不能为空");
        }
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单内容为空");
        }
        try {
            DeploymentEvent event = camundaClient.newDeployResourceCommand()
                    .addResourceStream(new ByteArrayInputStream(
                            schemaJson.getBytes(StandardCharsets.UTF_8)), formId + ".form")
                    .send()
                    .join();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("deploymentKey", event.getKey());
            if (event.getForm() != null) {
                out.put("forms", event.getForm().stream().map(f -> Map.of(
                        "formId", f.getFormId(),
                        "version", f.getVersion(),
                        "formKey", f.getFormKey())).toList());
            }
            return out;
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Camunda 表单部署失败: " + e.getMessage());
        }
    }

    /**
     * 把部署结果写进 process_binding。
     * <p>幂等：同 processKey 已有记录时只补 Camunda 侧字段；
     * **bpmnXml 为空时也会从 classpath 补上** —— 早期版本注册的绑定没有存 XML，
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
            path = "processes/ethnic-content-review.bpmn";
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

        ProcessInstanceEvent event;
        try {
            event = camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId(processId)
                    .latestVersion()
                    .businessId(businessKey)
                    .variables(variables)
                    .send()
                    .join();
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "启动审批流程失败（请确认 Camunda 已部署流程 " + processId + "）: " + e.getMessage());
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(UUID.randomUUID());
        instance.setEntryType(entryType);
        instance.setEntryId(entryId);
        instance.setEntryTitle(content.title());
        instance.setContentVersion(nextVersion);
        instance.setProcessDefinitionId(processId);
        instance.setProcessDefinitionVersion(null);
        instance.setProcessInstanceKey(event.getProcessInstanceKey());
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

        // 引擎侧尚未产生用户任务时也先把环节写正确，待办查询会以引擎为准做校正
        refreshInstanceFromEngine(instance);

        syncReviewSnapshot(entryType, entryId, WorkflowConstants.REVIEW_PENDING,
                instance.getId(), nextVersion, note);

        contentGateway.reindex(entryType, entryId);
        log.info("提交审批: entry={}#{} v{} instance={} processInstanceKey={}",
                entryType, entryId, nextVersion, instance.getId(), event.getProcessInstanceKey());
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

        // 引擎侧用户任务：用于拿到 taskKey / 任务名 / 真实表单
        Map<Long, UserTask> engineTasks = queryEngineUserTasks(
                instances.stream().map(WorkflowInstance::getProcessInstanceKey).filter(java.util.Objects::nonNull).toList());

        Set<String> myRoles = currentRoles();
        UUID me = currentUserId();
        List<WorkflowTaskResource> result = new ArrayList<>();
        for (WorkflowInstance instance : instances) {
            UserTask task = instance.getProcessInstanceKey() == null ? null
                    : engineTasks.get(instance.getProcessInstanceKey());
            String effectiveStage = task != null ? stageOfElement(task.getElementId()) : instance.getCurrentStage();
            if (task != null && !effectiveStage.equals(instance.getCurrentStage())) {
                // 引擎与本地不一致时以引擎为准并回写，避免界面显示过期环节
                instance.setCurrentStage(effectiveStage);
                instance.setCurrentTaskKey(task.getUserTaskKey());
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
        UserTask task = instance.getProcessInstanceKey() == null ? null
                : queryEngineUserTasks(List.of(instance.getProcessInstanceKey()))
                .get(instance.getProcessInstanceKey());
        String stage = task != null ? stageOfElement(task.getElementId()) : instance.getCurrentStage();
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
        UserTask task = currentEngineTask(instance);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前没有待处理的用户任务（流程可能已流转到下一步）");
        }
        String stage = stageOfElement(task.getElementId());
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
                        opinion, task.getUserTaskKey(), task.getName());
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
                }
                if (hasIssue && (opinion == null || opinion.isBlank())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "发现问题时必须填写审查意见");
                }
                addOpinion(instance, WorkflowConstants.OPINION_INSPECT,
                        hasIssue ? WorkflowConstants.DECISION_ISSUE : WorkflowConstants.DECISION_APPROVED,
                        opinion, task.getUserTaskKey(), task.getName());
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
                        task.getUserTaskKey(), task.getName());
                syncReviewSnapshot(instance.getEntryType(), instance.getEntryId(),
                        WorkflowConstants.REVIEW_PENDING, instance.getId(),
                        instance.getContentVersion(), opinion);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知环节: " + stage);
        }

        try {
            camundaClient.newCompleteUserTaskCommand(task.getUserTaskKey())
                    .variables(variables)
                    .send()
                    .join();
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "提交任务失败: " + e.getMessage());
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
                camundaClient.newCancelInstanceCommand(instance.getProcessInstanceKey()).send().join();
            } catch (ClientException e) {
                log.warn("取消 Camunda 流程实例失败（继续本地收尾）: {}", e.getMessage());
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
                    camundaClient.newCancelInstanceCommand(instance.getProcessInstanceKey()).send().join();
                } catch (ClientException e) {
                    log.warn("删除内容时取消流程失败: {}", e.getMessage());
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

    /** 引擎里当前待处理的用户任务 */
    private UserTask currentEngineTask(WorkflowInstance instance) {
        if (instance.getProcessInstanceKey() == null) {
            return null;
        }
        return queryEngineUserTasks(List.of(instance.getProcessInstanceKey()))
                .get(instance.getProcessInstanceKey());
    }

    /**
     * 查引擎中「未完成」的用户任务：processInstanceKey -> 该实例最早的待办任务。
     *
     * <p><b>为什么按实例全量拉取再本地过滤，而不用 filter.processInstanceKey：</b>
     * 本地跑的 Camunda 8.9 自管集群在 {@code /v2/user-tasks/search} 上无法解析
     * 超出 int 范围的流程实例 key（返回 <i>Request property [filter.processInstanceKey]
     * cannot be parsed</i>），Zeebe 生成的 key 都是 2^53 量级的长整型，因此服务器端过滤
     * 在这个版本上不可用。待办任务量很小（只查 state=CREATED），全量拉取后本地比对
     * 反而更稳，也顺带避免了 N 次查询。</p>
     */
    private Map<Long, UserTask> queryEngineUserTasks(List<Long> processInstanceKeys) {
        Map<Long, UserTask> map = new HashMap<>();
        if (processInstanceKeys == null || processInstanceKeys.isEmpty()) {
            return map;
        }
        Set<Long> wanted = processInstanceKeys.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (wanted.isEmpty()) {
            return map;
        }
        try {
            List<UserTask> items = camundaClient.newUserTaskSearchRequest()
                    .filter(f -> f.state(UserTaskState.CREATED))
                    .send()
                    .join()
                    .items();
            items.stream()
                    .filter(t -> wanted.contains(t.getProcessInstanceKey()))
                    .sorted(Comparator.comparing(UserTask::getCreationDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(t -> map.putIfAbsent(t.getProcessInstanceKey(), t));
        } catch (Exception e) {
            log.warn("查询 Camunda 用户任务失败（降级为本地环节）: {}", e.getMessage());
        }
        return map;
    }

    /**
     * 流程推进后，把引擎侧的当前任务/环节同步回本地实例。
     *
     * <p><b>为什么需要轮询等待：</b>Camunda 的用户任务完成后，后续服务任务（内容上线/下线）
     * 是异步执行的，此刻既查不到新的待办任务、流程实例也还没进入终态。若立刻下结论，
     * 就会把实例误判为「已结束」。这里按 {@link #ENGINE_SETTLE_ATTEMPTS} 次短轮询
     * 等到「出现新任务」或「流程进入终态」为止。</p>
     *
     * <p><b>注意事务边界：</b>本方法会在事务内 sleep，因此轮询总时长必须远小于
     * 数据库事务/连接超时（当前 12 × 250ms = 3s）。</p>
     */
    private void refreshInstanceFromEngine(WorkflowInstance instance) {
        if (instance.getProcessInstanceKey() == null) {
            return;
        }
        for (int attempt = 0; attempt < ENGINE_SETTLE_ATTEMPTS; attempt++) {
            UserTask task = queryEngineUserTasks(List.of(instance.getProcessInstanceKey()))
                    .get(instance.getProcessInstanceKey());
            if (task != null) {
                String stage = stageOfElement(task.getElementId());
                instance.setCurrentStage(stage);
                instance.setCurrentTaskKey(task.getUserTaskKey());
                instance.setCurrentTaskName(task.getName());
                instance.setCurrentAssigneeId(resolveAssigneeId(task, stage));
                instance.setStatus(WorkflowConstants.INSTANCE_RUNNING);
                instance.setUpdatedAt(LocalDateTime.now());
                entityQuery.updatable(instance).executeRows();
                return;
            }
            // 没有待办任务：可能正在跑服务任务（内容上线/下线），也可能流程已结束。
            if (isProcessInstanceEnded(instance.getProcessInstanceKey())) {
                finishInstance(instance);
                return;
            }
            if (attempt < ENGINE_SETTLE_ATTEMPTS - 1) {
                try {
                    Thread.sleep(ENGINE_SETTLE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // 轮询超时（引擎二次存储尚未追上）：保持 running，环节按业务语义推进，
        // 由兜底复查任务（WorkflowSettlementScheduler）稍后收尾，避免实例永久卡在中间态。
        log.info("实例 {} 在 {} 次轮询内未见新任务或终态，交由兜底复查任务收尾",
                instance.getId(), ENGINE_SETTLE_ATTEMPTS);
    }

    /** 流程实例是否已结束（COMPLETED / TERMINATED） */
    private boolean isProcessInstanceEnded(Long processInstanceKey) {
        try {
            var pi = camundaClient.newProcessInstanceGetRequest(processInstanceKey).send().join();
            if (pi.getState() == null) {
                return false;
            }
            String state = pi.getState().name();
            return "COMPLETED".equalsIgnoreCase(state) || "TERMINATED".equalsIgnoreCase(state);
        } catch (Exception e) {
            log.debug("查询流程实例状态失败（按未结束处理）: {}", e.getMessage());
            return false;
        }
    }

    /** 流程结束：读回内容状态决定 current_stage / status，并写一条收尾意见 */
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
                            Long taskKey, String taskName) {
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

    private UUID resolveAssigneeId(UserTask task, String stage) {
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

    private WorkflowTaskResource toTaskResource(WorkflowInstance instance, UserTask task,
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
                task == null ? instance.getCurrentTaskKey() : task.getUserTaskKey(),
                task == null ? instance.getCurrentTaskName() : task.getName(),
                task == null ? null : task.getElementId(),
                instance.getProcessDefinitionId(),
                task == null ? instance.getProcessDefinitionVersion() : task.getProcessDefinitionVersion(),
                instance.getProcessInstanceKey(),
                instance.getSubmitterName(),
                instance.getStartedAt(),
                instance.getUpdatedAt(),
                form == null ? null : form.getSchemaJson(),
                form == null ? null : form.getFormId(),
                actionable,
                WorkflowOpinionResource.of(opinions));
    }

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
}
