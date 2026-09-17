package com.czdr.work.service;

import com.czdr.work.model.entity.FormBinding;
import com.czdr.work.model.entity.ProcessBinding;
import com.czdr.work.model.request.FormBindingSaveRequest;
import com.czdr.work.model.request.ProcessBindingSaveRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Camunda 流程 / 表单定义管理（网页版 Modeler 的后端）。
 *
 * @author cz
 */
public interface WorkflowDefinitionService {

    // ==================== BPMN 流程定义 ====================

    List<ProcessBinding> listProcesses(String entryType);

    ProcessBinding getProcess(UUID id);

    /** 按业务类型取启用中的流程定义（无则返回 null，调用方回落到内置流程） */
    ProcessBinding findEnabledProcess(String entryType);

    /** 新建流程定义（version 自动 +1） */
    ProcessBinding saveProcess(ProcessBindingSaveRequest request);

    /** 更新指定流程定义 */
    ProcessBinding updateProcess(UUID id, ProcessBindingSaveRequest request);

    /** 删除流程定义（启用中的不允许删） */
    void deleteProcess(UUID id);

    /**
     * 把流程定义的 BPMN 部署到 Camunda 引擎，回填 camundaDefinitionId / version。
     */
    ProcessBinding deployProcess(UUID id);

    /** 转存为「另存新版本」：复制现有 XML 生成 version+1 的草稿 */
    ProcessBinding duplicateProcess(UUID id);

    /** 生成一个可用的最小 BPMN 模板（新建时前端编辑器的初始内容） */
    String buildStarterBpmn(String processKey, String processName, String entryType);

    // ==================== Camunda Form ====================

    List<FormBinding> listForms(String purpose, String entryType);

    FormBinding getForm(UUID id);

    /** 按 formId 取启用中的表单定义（取版本最高的） */
    FormBinding findFormByFormId(String formId);

    /** 按环节取表单（审批 / 审查 / 修改） */
    FormBinding findFormByPurpose(String purpose, String entryType);

    FormBinding saveForm(FormBindingSaveRequest request);

    FormBinding updateForm(UUID id, FormBindingSaveRequest request);

    void deleteForm(UUID id);

    /** 生成指定用途的 form-js schema 模板 */
    String buildStarterFormSchema(String purpose);

    /**
     * 生成指定用途 + 指定 formId 的 form-js schema 模板。
     * <p>Camunda 8 要求 {@code .form} 的 JSON 顶层带 {@code id}，且必须与 BPMN 里
     * userTask 引用的 formId 一致，故这里显式传入。</p>
     */
    String buildStarterFormSchema(String purpose, String formId);

    // ==================== 引擎侧只读视图 ====================

    /** Camunda 引擎上的流程定义列表（含未在 OA 注册的） */
    List<Map<String, Object>> listEngineProcessDefinitions();

    /** 引擎连通性 / 拓扑信息 */
    Map<String, Object> engineTopology();
}
