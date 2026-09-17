package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.FormBinding;
import com.czdr.work.model.entity.ProcessBinding;
import com.czdr.work.model.request.FormBindingSaveRequest;
import com.czdr.work.model.request.ProcessBindingSaveRequest;
import com.czdr.work.model.request.WorkflowOfflineRequest;
import com.czdr.work.model.request.WorkflowSubmitRequest;
import com.czdr.work.model.request.WorkflowTaskCompleteRequest;
import com.czdr.work.model.resource.WorkflowInstanceResource;
import com.czdr.work.model.resource.WorkflowOpinionResource;
import com.czdr.work.model.resource.WorkflowTaskResource;
import com.czdr.work.model.resource.WorkflowTimelineResource;
import com.czdr.work.service.WorkflowDefinitionService;
import com.czdr.work.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 内容审批工作流（Camunda 8）接口。
 *
 * <p>业务闭环：</p>
 * <pre>
 *  ① POST /api/workflow/entries/{entryType}/{entryId}/submit   内容编辑提交审批
 *  ② POST /api/workflow/instances/{id}/complete                审核员审批（留审批意见）
 *  ③ POST /api/workflow/instances/{id}/inspect/pass            内容管理员审查通过（保持在线）
 *     POST /api/workflow/instances/{id}/inspect/issue           发现问题 → 暂时下线
 *  ④ POST /api/workflow/instances/{id}/revise                  内容编辑修改完成 → 二次审批
 *  … 循环 ②→③→④
 * </pre>
 *
 * @author cz
 */
@Tag(name = "内容审批工作流 Workflow", description = "基于 Camunda 8 的内容审批闭环：提交 / 审批 / 上线 / 审查 / 下线 / 修改 / 重新上线，含审批审查意见与在线 Modeler")
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowDefinitionService definitionService;

    // ==================== 提交流程 ====================

    @Operation(summary = "提交内容审批", description = "内容编辑提交内容进入审批流程，流程实例启动并流转到「审核员审批」（需对应内容类型的 update 权限）")
    @SaCheckLogin
    @PostMapping("entries/{entryType}/{entryId}/submit")
    Result<WorkflowInstanceResource> submit(
            @PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String entryType,
            @PathVariable @Parameter(description = "内容 ID") UUID entryId,
            @RequestBody(required = false) WorkflowSubmitRequest request) {
        return Result.success(workflowService.submit(entryType, entryId,
                request == null ? null : request.note()));
    }

    @Operation(summary = "撤回提交", description = "提交人撤回尚未走完的审批流程，内容退回草稿")
    @SaCheckLogin
    @PostMapping("instances/{instanceId}/withdraw")
    Result<Void> withdraw(@PathVariable UUID instanceId,
                          @RequestBody(required = false) WorkflowOfflineRequest request) {
        workflowService.withdraw(instanceId, request == null ? null : request.reason());
        return Result.success(null);
    }

    // ==================== 待办与查询 ====================

    @Operation(summary = "我的待办", description = "查询当前登录人可处理的工作流任务；stage 可过滤 pending_review / pending_inspect / revising")
    @SaCheckLogin
    @GetMapping("tasks")
    Result<List<WorkflowTaskResource>> listTasks(
            @RequestParam(required = false) @Parameter(description = "环节：pending_review 待审批 / pending_inspect 待审查 / revising 待修改") String stage,
            @RequestParam(required = false) @Parameter(description = "内容类型：ethnic / festival / art / topic") String entryType,
            @RequestParam(defaultValue = "true") @Parameter(description = "是否只看我能处理的") boolean mineOnly) {
        return Result.success(workflowService.listTasks(stage, entryType, mineOnly));
    }

    @Operation(summary = "任务详情", description = "按工作流实例查询待办详情，含全量历史审批/审查意见与当前环节的 Camunda Form")
    @SaCheckLogin
    @GetMapping("instances/{instanceId}")
    Result<WorkflowTaskResource> getTask(@PathVariable UUID instanceId) {
        return Result.success(workflowService.getTaskByInstance(instanceId));
    }

    @Operation(summary = "待办（按内容）", description = "按内容类型 + 内容 ID 查询当前活跃的待办任务，无则返回 null")
    @SaCheckLogin
    @GetMapping("entries/{entryType}/{entryId}/task")
    Result<WorkflowTaskResource> getActiveTask(@PathVariable String entryType, @PathVariable UUID entryId) {
        return Result.success(workflowService.getActiveTaskByEntry(entryType, entryId));
    }

    @Operation(summary = "审批全过程", description = "按内容查询逐版本的流程实例与全部审批/审查意见，内容编辑据此查看前序意见")
    @SaCheckPermission("review:list")
    @GetMapping("entries/{entryType}/{entryId}/timeline")
    Result<WorkflowTimelineResource> getTimeline(@PathVariable String entryType, @PathVariable UUID entryId) {
        return Result.success(workflowService.getTimeline(entryType, entryId));
    }

    @Operation(summary = "审批审查意见列表", description = "按内容查询全部审批/审查意见（按时间正序）")
    @SaCheckPermission("review:list")
    @GetMapping("entries/{entryType}/{entryId}/opinions")
    Result<List<WorkflowOpinionResource>> listOpinions(@PathVariable String entryType, @PathVariable UUID entryId) {
        return Result.success(workflowService.listOpinions(entryType, entryId));
    }

    // ==================== 环节动作 ====================

    @Operation(summary = "完成当前环节", description = "通用入口：按当前环节执行审批 / 审查 / 修改。body 可带 decision + opinion + Camunda Form 变量")
    @SaCheckLogin
    @PostMapping("instances/{instanceId}/complete")
    Result<Void> complete(@PathVariable UUID instanceId,
                          @RequestBody(required = false) WorkflowTaskCompleteRequest request) {
        workflowService.completeTask(instanceId, request);
        return Result.success(null);
    }

    @Operation(summary = "审核员审批", description = "审核员提交审批意见；approved=true 上线并流转到内容管理员审查，false 退回内容编辑修改（需 review:approve 权限）")
    @SaCheckPermission("review:approve")
    @PostMapping("instances/{instanceId}/approve")
    Result<Void> approve(@PathVariable UUID instanceId,
                         @RequestBody(required = false) WorkflowTaskCompleteRequest request) {
        WorkflowTaskCompleteRequest merged = request == null
                ? new WorkflowTaskCompleteRequest("approved", null, null, null, null)
                : new WorkflowTaskCompleteRequest("approved", request.opinion(), request.rejectReason(),
                request.comment(), request.variables());
        workflowService.completeTask(instanceId, merged);
        return Result.success(null);
    }

    @Operation(summary = "审核员退回", description = "审核员退回内容交内容编辑修改，必须填写审批意见（需 review:reject 权限）")
    @SaCheckPermission("review:reject")
    @PostMapping("instances/{instanceId}/reject")
    Result<Void> reject(@PathVariable UUID instanceId,
                        @RequestBody(required = false) WorkflowTaskCompleteRequest request) {
        WorkflowTaskCompleteRequest merged = request == null
                ? new WorkflowTaskCompleteRequest("rejected", null, null, null, null)
                : new WorkflowTaskCompleteRequest("rejected", request.opinion(), request.rejectReason(),
                request.comment(), request.variables());
        workflowService.completeTask(instanceId, merged);
        return Result.success(null);
    }

    @Operation(summary = "内容管理员审查通过", description = "审查无问题，内容保持在线，本轮闭环结束（需 review:inspect 权限）")
    @SaCheckPermission("review:inspect")
    @PostMapping("instances/{instanceId}/inspect/pass")
    Result<Void> inspectPass(@PathVariable UUID instanceId,
                             @RequestBody(required = false) WorkflowTaskCompleteRequest request) {
        workflowService.inspectPass(instanceId, request == null ? null : request.resolveOpinion());
        return Result.success(null);
    }

    @Operation(summary = "内容管理员审查发现问题并下线", description = "内容暂时下线，交内容编辑修改；必须填写审查意见（需 review:offline 权限）")
    @SaCheckPermission("review:offline")
    @PostMapping("instances/{instanceId}/inspect/issue")
    Result<Void> inspectIssue(@PathVariable UUID instanceId,
                              @RequestBody WorkflowOfflineRequest request) {
        workflowService.inspectIssueAndOffline(instanceId, request == null ? null : request.reason());
        return Result.success(null);
    }

    @Operation(summary = "内容编辑修改完成", description = "内容编辑填写修改说明并重新提交审核员二次审批（needReapproval=false 时直接重新上线）（需对应内容类型的 update 权限）")
    @SaCheckLogin
    @PostMapping("instances/{instanceId}/revise")
    Result<Void> revise(@PathVariable UUID instanceId,
                        @RequestBody(required = false) WorkflowTaskCompleteRequest request) {
        boolean needReapproval = request == null || request.variables() == null
                || !"false".equalsIgnoreCase(String.valueOf(request.variables().get("needReapproval")));
        workflowService.reviseAndResubmit(instanceId,
                request == null ? null : request.resolveOpinion(), needReapproval);
        return Result.success(null);
    }

    // ==================== 网页版 Modeler：流程定义 ====================

    @Operation(summary = "流程定义列表", description = "查询 OA 中登记的 BPMN 流程定义（网页版 Modeler）")
    @SaCheckPermission("workflow:list")
    @GetMapping("processes")
    Result<List<ProcessBinding>> listProcesses(
            @RequestParam(required = false) @Parameter(description = "业务类型：ethnic / festival / art / topic / *") String entryType) {
        return Result.success(definitionService.listProcesses(entryType));
    }

    @Operation(summary = "流程定义详情", description = "按 ID 查询流程定义（含 BPMN XML 原文）")
    @SaCheckPermission("workflow:list")
    @GetMapping("processes/{id}")
    Result<ProcessBinding> getProcess(@PathVariable UUID id) {
        return Result.success(definitionService.getProcess(id));
    }

    @Operation(summary = "新建流程定义", description = "保存一个新的 BPMN 流程定义（版本自动 +1）；enabled=true 会停用同业务类型的其他版本（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @PostMapping("processes")
    Result<ProcessBinding> saveProcess(@RequestBody ProcessBindingSaveRequest request) {
        return Result.success(definitionService.saveProcess(request));
    }

    @Operation(summary = "更新流程定义", description = "更新 BPMN XML / 名称 / 启用状态（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @PutMapping("processes/{id}")
    Result<ProcessBinding> updateProcess(@PathVariable UUID id, @RequestBody ProcessBindingSaveRequest request) {
        return Result.success(definitionService.updateProcess(id, request));
    }

    @Operation(summary = "删除流程定义", description = "删除未被启用的流程定义（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @DeleteMapping("processes/{id}")
    Result<Void> deleteProcess(@PathVariable UUID id) {
        definitionService.deleteProcess(id);
        return Result.success(null);
    }

    @Operation(summary = "部署流程定义", description = "把该 BPMN 部署到 Camunda 引擎并回填引擎版本号（需 workflow:deploy 权限）")
    @SaCheckPermission("workflow:deploy")
    @PostMapping("processes/{id}/deploy")
    Result<ProcessBinding> deployProcess(@PathVariable UUID id) {
        return Result.success(definitionService.deployProcess(id));
    }

    @Operation(summary = "另存新版本", description = "复制当前 BPMN 生成 version+1 的草稿版本（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @PostMapping("processes/{id}/duplicate")
    Result<ProcessBinding> duplicateProcess(@PathVariable UUID id) {
        return Result.success(definitionService.duplicateProcess(id));
    }

    @Operation(summary = "BPMN 模板", description = "生成可用于新建流程的 BPMN 模板 XML")
    @SaCheckPermission("workflow:list")
    @GetMapping("processes/template")
    Result<String> processTemplate(
            @RequestParam(defaultValue = "custom-content-review") String processKey,
            @RequestParam(defaultValue = "内容审批流程") String processName,
            @RequestParam(defaultValue = "*") String entryType) {
        return Result.success(definitionService.buildStarterBpmn(processKey, processName, entryType));
    }

    // ==================== 网页版 Modeler：Camunda Form ====================

    @Operation(summary = "Camunda Form 列表", description = "查询 form-js 表单定义（审批 / 审查 / 修改）")
    @SaCheckPermission("workflow:list")
    @GetMapping("forms")
    Result<List<FormBinding>> listForms(
            @RequestParam(required = false) @Parameter(description = "用途：approval / inspection / revision") String purpose,
            @RequestParam(required = false) @Parameter(description = "业务类型") String entryType) {
        return Result.success(definitionService.listForms(purpose, entryType));
    }

    @Operation(summary = "Camunda Form 详情", description = "按 ID 查询表单定义（含 form-js schema JSON 原文）")
    @SaCheckPermission("workflow:list")
    @GetMapping("forms/{id}")
    Result<FormBinding> getForm(@PathVariable UUID id) {
        return Result.success(definitionService.getForm(id));
    }

    @Operation(summary = "按 formId 查询表单", description = "按 Camunda Form 的 formId 查询启用中的 schema，供 FormRenderer 渲染")
    @SaCheckLogin
    @GetMapping("forms/by-form-id/{formId}")
    Result<FormBinding> getFormByFormId(@PathVariable String formId) {
        return Result.success(definitionService.findFormByFormId(formId));
    }

    @Operation(summary = "新建 Camunda Form", description = "保存新的 form-js 表单定义（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @PostMapping("forms")
    Result<FormBinding> saveForm(@RequestBody FormBindingSaveRequest request) {
        return Result.success(definitionService.saveForm(request));
    }

    @Operation(summary = "更新 Camunda Form", description = "更新表单 schema / 名称 / 启用状态（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @PutMapping("forms/{id}")
    Result<FormBinding> updateForm(@PathVariable UUID id, @RequestBody FormBindingSaveRequest request) {
        return Result.success(definitionService.updateForm(id, request));
    }

    @Operation(summary = "删除 Camunda Form", description = "删除未被启用的表单定义（需 workflow:save 权限）")
    @SaCheckPermission("workflow:save")
    @DeleteMapping("forms/{id}")
    Result<Void> deleteForm(@PathVariable UUID id) {
        definitionService.deleteForm(id);
        return Result.success(null);
    }

    @Operation(summary = "表单 schema 模板", description = "按用途（approval / inspection / revision）生成 form-js schema 模板；formId 需与 BPMN 中 userTask 引用一致")
    @SaCheckPermission("workflow:list")
    @GetMapping("forms/template")
    Result<String> formTemplate(
            @RequestParam(defaultValue = "approval") String purpose,
            @RequestParam(required = false) @Parameter(description = "formId，留空按用途取默认值") String formId) {
        return Result.success(formId == null || formId.isBlank()
                ? definitionService.buildStarterFormSchema(purpose)
                : definitionService.buildStarterFormSchema(purpose, formId));
    }

    // ==================== 引擎信息 ====================

    @Operation(summary = "Camunda 引擎拓扑", description = "查询 Camunda 8 引擎版本与集群健康状态（需 workflow:list 权限）")
    @SaCheckPermission("workflow:list")
    @GetMapping("engine/topology")
    Result<Map<String, Object>> topology() {
        return Result.success(definitionService.engineTopology());
    }

    @Operation(summary = "引擎流程定义", description = "查询 Camunda 引擎上已部署的流程定义（需 workflow:list 权限）")
    @SaCheckPermission("workflow:list")
    @GetMapping("engine/process-definitions")
    Result<List<Map<String, Object>>> engineProcessDefinitions() {
        return Result.success(definitionService.listEngineProcessDefinitions());
    }

    @Operation(summary = "重新部署内置流程", description = "把 classpath 内置的 BPMN 与表单重新部署到 Camunda（需 workflow:deploy 权限）")
    @SaCheckPermission("workflow:deploy")
    @PostMapping("engine/redeploy-builtin")
    Result<Map<String, Object>> redeployBuiltin() {
        return Result.success(workflowService.deployBuiltinResources());
    }

    @Operation(summary = "部署前校验 BPMN", description = "把 BPMN XML 原样发给 Camunda 校验并部署，引擎错误直接回显给建模人（需 workflow:deploy 权限）")
    @SaCheckPermission("workflow:deploy")
    @PostMapping("engine/validate-bpmn")
    Result<Map<String, Object>> validateBpmn(
            @RequestParam(defaultValue = "custom-process.bpmn") String resourceName,
            @RequestBody String bpmnXml) {
        return Result.success(workflowService.deployBpmn(resourceName, bpmnXml));
    }

    @Operation(summary = "部署 Camunda Form 到引擎", description = "把指定 formId 的 form-js schema 部署到 Camunda（流程里 userTask 引用的表单必须已部署，否则任务创建会抛 FORM_NOT_FOUND）（需 workflow:deploy 权限）")
    @SaCheckPermission("workflow:deploy")
    @PostMapping("forms/{id}/deploy")
    Result<Map<String, Object>> deployForm(@PathVariable UUID id) {
        FormBinding binding = definitionService.getForm(id);
        return Result.success(workflowService.deployForm(binding.getFormId(), binding.getSchemaJson()));
    }
}
