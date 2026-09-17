package com.czdr.work.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.FormBinding;
import com.czdr.work.model.entity.ProcessBinding;
import com.czdr.work.model.request.FormBindingSaveRequest;
import com.czdr.work.model.request.ProcessBindingSaveRequest;
import com.czdr.work.service.WorkflowDefinitionService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.response.DeploymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 网页版 Camunda Modeler 的后端：BPMN 流程与 Camunda Form 的持久化 + 部署。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private final EasyEntityQuery entityQuery;
    private final CamundaClient camundaClient;

    // ==================================================================================
    // BPMN 流程定义
    // ==================================================================================

    @Override
    public List<ProcessBinding> listProcesses(String entryType) {
        return entityQuery.queryable(ProcessBinding.class)
                .where(b -> {
                    if (entryType != null && !entryType.isBlank()) {
                        b.entryType().eq(entryType);
                    }
                })
                .orderBy(b -> b.processKey().asc())
                .orderBy(b -> b.version().desc())
                .toList();
    }

    @Override
    public ProcessBinding getProcess(UUID id) {
        ProcessBinding binding = entityQuery.queryable(ProcessBinding.class)
                .where(b -> b.id().eq(id)).firstOrNull();
        if (binding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程定义不存在");
        }
        return binding;
    }

    @Override
    public ProcessBinding findEnabledProcess(String entryType) {
        ProcessBinding binding = entityQuery.queryable(ProcessBinding.class)
                .where(b -> {
                    b.entryType().eq(entryType);
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
        if (binding != null) {
            return binding;
        }
        // 回落：通用流程（entryType = *）
        return entityQuery.queryable(ProcessBinding.class)
                .where(b -> {
                    b.entryType().eq("*");
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
    }

    @Override
    @Transactional
    public ProcessBinding saveProcess(ProcessBindingSaveRequest request) {
        validateBpmn(request.bpmnXml());
        String processKey = blankTo(request.processKey(), "custom-content-review");
        int nextVersion = nextProcessVersion(processKey);

        ProcessBinding binding = new ProcessBinding();
        binding.setId(UUID.randomUUID());
        binding.setProcessKey(processKey);
        binding.setName(blankTo(request.name(), processKey));
        binding.setEntryType(blankTo(request.entryType(), "*"));
        binding.setVersion(nextVersion);
        binding.setBpmnXml(request.bpmnXml());
        binding.setEnabled(Boolean.TRUE.equals(request.enabled()));
        binding.setRemark(request.remark());
        binding.setCreatedBy(currentUserId());
        binding.setUpdatedBy(currentUserId());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(binding).executeRows();

        if (Boolean.TRUE.equals(binding.getEnabled())) {
            disableOthers(binding.getEntryType(), binding.getId());
        }
        return binding;
    }

    @Override
    @Transactional
    public ProcessBinding updateProcess(UUID id, ProcessBindingSaveRequest request) {
        ProcessBinding binding = getProcess(id);
        if (request.bpmnXml() != null) {
            validateBpmn(request.bpmnXml());
            binding.setBpmnXml(request.bpmnXml());
        }
        if (request.name() != null) {
            binding.setName(request.name());
        }
        if (request.entryType() != null && !request.entryType().isBlank()) {
            binding.setEntryType(request.entryType());
        }
        if (request.remark() != null) {
            binding.setRemark(request.remark());
        }
        if (request.enabled() != null) {
            binding.setEnabled(request.enabled());
        }
        binding.setUpdatedBy(currentUserId());
        binding.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(binding).executeRows();

        if (Boolean.TRUE.equals(binding.getEnabled())) {
            disableOthers(binding.getEntryType(), binding.getId());
        }
        return binding;
    }

    @Override
    @Transactional
    public void deleteProcess(UUID id) {
        ProcessBinding binding = getProcess(id);
        if (Boolean.TRUE.equals(binding.getEnabled())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "启用中的流程定义不能删除，请先停用或切换到其他版本");
        }
        entityQuery.deletable(binding).allowDeleteStatement(true).executeRows();
    }

    @Override
    @Transactional
    public ProcessBinding deployProcess(UUID id) {
        ProcessBinding binding = getProcess(id);
        validateBpmn(binding.getBpmnXml());

        String resourceName = binding.getProcessKey() + ".bpmn";
        try {
            DeploymentEvent event = camundaClient.newDeployResourceCommand()
                    .addResourceStream(
                            new ByteArrayInputStream(binding.getBpmnXml().getBytes(StandardCharsets.UTF_8)),
                            resourceName)
                    .send()
                    .join();
            event.getProcesses().stream()
                    .filter(p -> p.getBpmnProcessId().equals(binding.getProcessKey()))
                    .findFirst()
                    .ifPresent(p -> {
                        binding.setCamundaDefinitionId(p.getBpmnProcessId());
                        binding.setCamundaDefinitionVersion(p.getVersion());
                    });
            binding.setUpdatedBy(currentUserId());
            binding.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(binding).executeRows();
            log.info("流程定义部署成功: {} v{} -> engine v{}", binding.getProcessKey(),
                    binding.getVersion(), binding.getCamundaDefinitionVersion());
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "部署失败: " + e.getMessage());
        }
        return binding;
    }

    @Override
    @Transactional
    public ProcessBinding duplicateProcess(UUID id) {
        ProcessBinding source = getProcess(id);
        ProcessBinding copy = new ProcessBinding();
        copy.setId(UUID.randomUUID());
        copy.setProcessKey(source.getProcessKey());
        copy.setName(source.getName() + "（副本）");
        copy.setEntryType(source.getEntryType());
        copy.setVersion(nextProcessVersion(source.getProcessKey()));
        copy.setBpmnXml(source.getBpmnXml());
        copy.setEnabled(false);
        copy.setRemark(source.getRemark());
        copy.setCreatedBy(currentUserId());
        copy.setUpdatedBy(currentUserId());
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(copy).executeRows();
        return copy;
    }

    @Override
    public String buildStarterBpmn(String processKey, String processName, String entryType) {
        String key = blankTo(processKey, "custom-content-review");
        String name = blankTo(processName, "内容审批流程");
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"
                                  xmlns:modeler="http://camunda.org/schema/modeler/1.0"
                                  id="Definitions_%1$s"
                                  targetNamespace="http://bpmn.io/schema/bpmn"
                                  exporter="56民族-OA 内置 Modeler"
                                  modeler:executionPlatform="Camunda Cloud"
                                  modeler:executionPlatformVersion="8.9.0">
                  <bpmn:process id="%1$s" name="%2$s" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_1" name="提交">
                      <bpmn:outgoing>Flow_1</bpmn:outgoing>
                    </bpmn:startEvent>
                    <bpmn:userTask id="Task_ReviewerApprove" name="审核员审批" zeebe:candidateGroups="reviewer">
                      <bpmn:extensionElements>
                        <zeebe:userTask />
                        <zeebe:formDefinition formId="ethnic-approval-form" />
                      </bpmn:extensionElements>
                      <bpmn:incoming>Flow_1</bpmn:incoming>
                      <bpmn:outgoing>Flow_2</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:endEvent id="EndEvent_1" name="结束">
                      <bpmn:incoming>Flow_2</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_ReviewerApprove" />
                    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_ReviewerApprove" targetRef="EndEvent_1" />
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="%1$s">
                      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
                        <dc:Bounds x="162" y="102" width="36" height="36" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Task_ReviewerApprove_di" bpmnElement="Task_ReviewerApprove">
                        <dc:Bounds x="260" y="80" width="100" height="80" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
                        <dc:Bounds x="422" y="102" width="36" height="36" />
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
                        <di:waypoint x="198" y="120" />
                        <di:waypoint x="260" y="120" />
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
                        <di:waypoint x="360" y="120" />
                        <di:waypoint x="422" y="120" />
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """.formatted(key, name);
    }

    // ==================================================================================
    // Camunda Form
    // ==================================================================================

    @Override
    public List<FormBinding> listForms(String purpose, String entryType) {
        return entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    if (purpose != null && !purpose.isBlank()) {
                        b.purpose().eq(purpose);
                    }
                    if (entryType != null && !entryType.isBlank()) {
                        b.entryType().eq(entryType);
                    }
                })
                .orderBy(b -> b.formId().asc())
                .orderBy(b -> b.version().desc())
                .toList();
    }

    @Override
    public FormBinding getForm(UUID id) {
        FormBinding binding = entityQuery.queryable(FormBinding.class)
                .where(b -> b.id().eq(id)).firstOrNull();
        if (binding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "表单定义不存在");
        }
        return binding;
    }

    @Override
    public FormBinding findFormByFormId(String formId) {
        return entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    b.formId().eq(formId);
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
    }

    @Override
    public FormBinding findFormByPurpose(String purpose, String entryType) {
        FormBinding exact = entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    b.purpose().eq(purpose);
                    b.entryType().eq(entryType);
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
        if (exact != null) {
            return exact;
        }
        // 回落：该用途下业务类型为 * 的通用表单
        FormBinding generic = entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    b.purpose().eq(purpose);
                    b.entryType().eq("*");
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
        if (generic != null) {
            return generic;
        }
        // 再回落：该业务类型下任何启用表单
        return entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    b.entryType().eq(entryType);
                    b.enabled().eq(true);
                })
                .orderBy(b -> b.version().desc())
                .firstOrNull();
    }

    @Override
    @Transactional
    public FormBinding saveForm(FormBindingSaveRequest request) {
        validateFormSchema(request.schemaJson());
        String formId = blankTo(request.formId(), "custom-form-" + UUID.randomUUID().toString().substring(0, 8));
        int nextVersion = nextFormVersion(formId);

        FormBinding binding = new FormBinding();
        binding.setId(UUID.randomUUID());
        binding.setFormId(formId);
        binding.setName(blankTo(request.name(), formId));
        binding.setPurpose(blankTo(request.purpose(), "approval"));
        binding.setEntryType(blankTo(request.entryType(), "*"));
        binding.setVersion(nextVersion);
        binding.setSchemaJson(request.schemaJson());
        binding.setEnabled(Boolean.TRUE.equals(request.enabled()));
        binding.setRemark(request.remark());
        binding.setCreatedBy(currentUserId());
        binding.setUpdatedBy(currentUserId());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(binding).executeRows();

        if (Boolean.TRUE.equals(binding.getEnabled())) {
            disableOtherForms(binding.getPurpose(), binding.getEntryType(), binding.getId());
        }
        return binding;
    }

    @Override
    @Transactional
    public FormBinding updateForm(UUID id, FormBindingSaveRequest request) {
        FormBinding binding = getForm(id);
        if (request.schemaJson() != null) {
            validateFormSchema(request.schemaJson());
            binding.setSchemaJson(request.schemaJson());
        }
        if (request.name() != null) {
            binding.setName(request.name());
        }
        if (request.purpose() != null && !request.purpose().isBlank()) {
            binding.setPurpose(request.purpose());
        }
        if (request.entryType() != null && !request.entryType().isBlank()) {
            binding.setEntryType(request.entryType());
        }
        if (request.remark() != null) {
            binding.setRemark(request.remark());
        }
        if (request.enabled() != null) {
            binding.setEnabled(request.enabled());
        }
        binding.setUpdatedBy(currentUserId());
        binding.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(binding).executeRows();

        if (Boolean.TRUE.equals(binding.getEnabled())) {
            disableOtherForms(binding.getPurpose(), binding.getEntryType(), binding.getId());
        }
        return binding;
    }

    @Override
    @Transactional
    public void deleteForm(UUID id) {
        FormBinding binding = getForm(id);
        if (Boolean.TRUE.equals(binding.getEnabled())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "启用中的表单不能删除，请先停用");
        }
        entityQuery.deletable(binding).allowDeleteStatement(true).executeRows();
    }

    @Override
    public String buildStarterFormSchema(String purpose) {
        return buildStarterFormSchema(purpose, defaultFormId(purpose));
    }

    /** 用途 -> 默认 formId（与 BPMN 中 userTask 的 zeebe:formDefinition formId 一致） */
    private String defaultFormId(String purpose) {
        return switch (purpose == null ? "approval" : purpose) {
            case "inspection" -> "ethnic-inspection-form";
            case "revision" -> "ethnic-revision-form";
            default -> "ethnic-approval-form";
        };
    }

    /**
     * 生成 form-js schema。
     * <p><b>注意</b>：Camunda 8 部署 {@code .form} 资源时要求 JSON 顶层带 {@code id}，
     * 否则引擎报「Expected the form id to be present, but none given」；
     * 该 id 必须与流程里 userTask 引用的 formId 一致，否则任务创建时会抛 FORM_NOT_FOUND。</p>
     */
    @Override
    public String buildStarterFormSchema(String purpose, String formId) {
        String id = blankTo(formId, defaultFormId(purpose));
        return switch (purpose == null ? "approval" : purpose) {
            case "inspection" -> """
                    {
                      "id": "%1$s",
                      "schemaVersion": 17,
                      "type": "default",
                      "components": [
                        {"type":"text","id":"inspectionTitle","label":"内容管理员审查","layout":{"row":"","columns":null}},
                        {"type":"textfield","id":"entryTitle","key":"entryTitle","label":"内容名称","readonly":true,"layout":{"row":"","columns":null}},
                        {"type":"textarea","id":"inspectionOpinion","key":"inspectionOpinion","label":"审查意见","description":"如发现问题，请写明需修改之处；内容将暂时下线并交内容编辑修改","validate":{"required":true},"layout":{"row":"","columns":null}},
                        {"type":"checklist","id":"decision","key":"decision","label":"审查结论","values":[{"label":"无问题，保持在线","value":"approved"},{"label":"发现问题，暂时下线","value":"issue"}],"validate":{"required":true},"layout":{"row":"","columns":null}}
                      ]
                    }
                    """.formatted(id);
            case "revision" -> """
                    {
                      "id": "%1$s",
                      "schemaVersion": 17,
                      "type": "default",
                      "components": [
                        {"type":"text","id":"revisionTitle","label":"内容编辑修改","layout":{"row":"","columns":null}},
                        {"type":"textarea","id":"revisionNote","key":"revisionNote","label":"修改说明","description":"请说明本次针对审批/审查意见做了哪些修改","validate":{"required":true},"layout":{"row":"","columns":null}},
                        {"type":"checkbox","id":"needReapproval","key":"needReapproval","label":"修改完成后再次提交审核员审批","defaultValue":true,"layout":{"row":"","columns":null}}
                      ]
                    }
                    """.formatted(id);
            default -> """
                    {
                      "id": "%1$s",
                      "schemaVersion": 17,
                      "type": "default",
                      "components": [
                        {"type":"text","id":"approvalTitle","label":"审核员审批","layout":{"row":"","columns":null}},
                        {"type":"textfield","id":"entryTitle","key":"entryTitle","label":"内容名称","readonly":true,"layout":{"row":"","columns":null}},
                        {"type":"textarea","id":"approvalOpinion","key":"approvalOpinion","label":"审批意见","description":"请填写审批意见，将留存并展示给内容编辑","validate":{"required":true},"layout":{"row":"","columns":null}},
                        {"type":"checklist","id":"decision","key":"decision","label":"审批结论","values":[{"label":"通过","value":"approved"},{"label":"退回修改","value":"rejected"}],"validate":{"required":true},"layout":{"row":"","columns":null}}
                      ]
                    }
                    """.formatted(id);
        };
    }

    // ==================================================================================
    // 引擎只读视图
    // ==================================================================================

    @Override
    public List<Map<String, Object>> listEngineProcessDefinitions() {
        try {
            return camundaClient.newProcessDefinitionSearchRequest()
                    .send()
                    .join()
                    .items()
                    .stream()
                    .map(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("processDefinitionId", p.getProcessDefinitionId());
                        m.put("processDefinitionKey", p.getProcessDefinitionKey());
                        m.put("name", p.getName());
                        m.put("version", p.getVersion());
                        m.put("resourceName", p.getResourceName());
                        m.put("tenantId", p.getTenantId());
                        return m;
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("查询引擎流程定义失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> engineTopology() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var topology = camundaClient.newTopologyRequest().send().join();
            result.put("gatewayVersion", topology.getGatewayVersion());
            result.put("clusterSize", topology.getClusterSize());
            result.put("partitionsCount", topology.getPartitionsCount());
            result.put("replicationFactor", topology.getReplicationFactor());
            result.put("brokers", topology.getBrokers().stream().map(b -> Map.of(
                    "nodeId", b.getNodeId(),
                    "host", b.getHost(),
                    "port", b.getPort(),
                    "version", b.getVersion())).toList());
            result.put("healthy", true);
        } catch (Exception e) {
            result.put("healthy", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ==================================================================================
    // 校验与小工具
    // ==================================================================================

    /** 轻量 BPMN 校验：确保是 XML、含 bpmn:definitions、含可执行 process 与 id */
    private void validateBpmn(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "BPMN 内容为空");
        }
        if (!xml.contains("<bpmn:definitions") && !xml.contains("<definitions")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不是合法的 BPMN 文件（缺少 definitions 根节点）");
        }
        if (!xml.contains("<bpmn:process") && !xml.contains("<process")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "BPMN 缺少 process 节点");
        }
        if (!xml.contains("isExecutable=\"true\"")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流程必须包含 isExecutable=\"true\" 的 process 才能部署");
        }
    }

    /** 轻量 form-js schema 校验 */
    private void validateFormSchema(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单内容为空");
        }
        if (!json.trim().startsWith("{")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单 schema 必须是 JSON 对象");
        }
        if (!json.contains("\"components\"")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单 schema 缺少 components 字段");
        }
    }

    private void disableOthers(String entryType, UUID keepId) {
        List<ProcessBinding> others = entityQuery.queryable(ProcessBinding.class)
                .where(b -> {
                    b.entryType().eq(entryType);
                    b.id().ne(keepId);
                    b.enabled().eq(true);
                })
                .toList();
        for (ProcessBinding other : others) {
            other.setEnabled(false);
            other.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(other).executeRows();
        }
    }

    private void disableOtherForms(String purpose, String entryType, UUID keepId) {
        List<FormBinding> others = entityQuery.queryable(FormBinding.class)
                .where(b -> {
                    b.purpose().eq(purpose);
                    b.entryType().eq(entryType);
                    b.id().ne(keepId);
                    b.enabled().eq(true);
                })
                .toList();
        for (FormBinding other : others) {
            other.setEnabled(false);
            other.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(other).executeRows();
        }
    }

    private int nextProcessVersion(String processKey) {
        ProcessBinding latest = entityQuery.queryable(ProcessBinding.class)
                .where(b -> b.processKey().eq(processKey))
                .orderBy(b -> b.version().desc())
                .firstOrNull();
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    private int nextFormVersion(String formId) {
        FormBinding latest = entityQuery.queryable(FormBinding.class)
                .where(b -> b.formId().eq(formId))
                .orderBy(b -> b.version().desc())
                .firstOrNull();
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private UUID currentUserId() {
        try {
            return UUID.fromString(StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            return null;
        }
    }
}
