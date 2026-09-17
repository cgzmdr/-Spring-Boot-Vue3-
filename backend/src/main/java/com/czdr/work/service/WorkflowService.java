package com.czdr.work.service;

import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.model.resource.WorkflowInstanceResource;
import com.czdr.work.model.resource.WorkflowOpinionResource;
import com.czdr.work.model.resource.WorkflowTaskResource;
import com.czdr.work.model.resource.WorkflowTimelineResource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 内容审批工作流（Camunda 8）服务。
 *
 * <p>职责划分：</p>
 * <ul>
 *   <li>Camunda 负责「流转」——用户任务、排他网关、服务任务，流程定义可在线建模；</li>
 *   <li>本服务负责「业务语义」——提交、审批、上线、审查、暂时下线、修改、重新上线，
 *       并在每一步把业务状态、内容状态、审批/审查意见三者对齐。</li>
 * </ul>
 *
 * @author cz
 */
public interface WorkflowService {

    // ==================== 流程定义 / 部署 ====================

    /**
     * 部署 classpath 下的内置流程资源（民族审批流程 + 表单集合）到 Camunda，
     * 并把绑定信息落到 process_binding / form_binding。启动时调用，幂等。
     *
     * @return 部署的资源摘要（资源名 -> 版本），失败时抛出业务异常
     */
    Map<String, Object> deployBuiltinResources();

    /**
     * 部署单份 BPMN 内容（不落库）。
     * <p>供「Camunda Modeler」页做**部署前校验**：把 XML 原样发给引擎，
     * 引擎的校验错误（XSD、缺少可执行流程、表达式语法等）能直接回显给建模人。</p>
     *
     * @param resourceName 资源名（引擎侧展示用，如 my-process.bpmn）
     * @param bpmnXml      BPMN XML 原文
     * @return 部署结果摘要（processDefinitionId / version / key）
     */
    Map<String, Object> deployBpmn(String resourceName, String bpmnXml);

    /**
     * 部署单份 Camunda Form（form-js schema）到引擎。
     * <p>Camunda 8 中表单一经被 userTask 引用就必须真实存在，否则任务创建时
     * 会抛 FORM_NOT_FOUND incident；因此在 Modeler 里保存表单后应立即部署。</p>
     *
     * @param formId     Camunda Form 的 formId（需与 BPMN 中 formId 一致）
     * @param schemaJson form-js schema JSON 原文
     * @return 部署结果摘要（formId / version / formKey）
     */
    Map<String, Object> deployForm(String formId, String schemaJson);

    // ==================== 提交审批 ====================

    /**
     * 内容编辑提交审批：启动流程实例，落一条「提交审批」意见，内容状态置为 pending。
     *
     * @param entryType 内容类型（ethnic / festival / art / topic）
     * @param entryId   内容 ID
     * @param note      提交说明（可空）
     * @return 新建的工作流实例
     */
    WorkflowInstanceResource submit(String entryType, UUID entryId, String note);

    // ==================== 待办与查询 ====================

    /**
     * 查询待办：按「当前登录人可处理的环节」筛选。
     *
     * @param stage    环节过滤（可空 = 全部）：pending_review / pending_inspect / revising
     * @param entryType 内容类型过滤（可空）
     * @param mineOnly 仅看「我负责的 / 我提交的」
     */
    List<WorkflowTaskResource> listTasks(String stage, String entryType, boolean mineOnly);

    /** 待办任务详情（含全量历史意见 + 当前环节表单 schema） */
    WorkflowTaskResource getTaskByInstance(UUID instanceId);

    /** 按内容查待办任务（当前活跃实例） */
    WorkflowTaskResource getActiveTaskByEntry(String entryType, UUID entryId);

    /** 内容审批全过程（逐版本实例 + 全量意见），供内容编辑查看前序审批/审查意见 */
    WorkflowTimelineResource getTimeline(String entryType, UUID entryId);

    /** 按内容查全部意见（按时间正序） */
    List<WorkflowOpinionResource> listOpinions(String entryType, UUID entryId);

    // ==================== 环节动作 ====================

    /**
     * 完成当前环节任务（审批 / 审查 / 修改 通用入口）。
     *
     * @param instanceId 工作流实例 ID
     * @param request    结论 + 意见 + Camunda Form 变量
     */
    void completeTask(UUID instanceId, com.czdr.work.model.request.WorkflowTaskCompleteRequest request);

    /**
     * 内容管理员审查通过（保持在线），本轮闭环结束。
     */
    void inspectPass(UUID instanceId, String opinion);

    /**
     * 内容管理员审查发现问题：内容暂时下线，交内容编辑修改（进入 revising 环节）。
     */
    void inspectIssueAndOffline(UUID instanceId, String reason);

    /**
     * 内容编辑修改完成并重新提交审核员二次审批。
     */
    void reviseAndResubmit(UUID instanceId, String revisionNote, boolean needReapproval);

    /** 撤回提交（仅提交人，流程尚未走完时） */
    void withdraw(UUID instanceId, String reason);

    // ==================== 供 AdminService 内部调用 ====================

    /**
     * 校验内容是否允许提交审批（已在线/已在审批中则拒绝），并返回可提交性.
     */
    void assertSubmittable(String entryType, UUID entryId);

    /**
     * 内容状态变更后同步工作流实例（如删除内容时终止流程）。
     */
    void onContentDeleted(String entryType, UUID entryId);

    /** 同步 content_review 审核态快照（列表页 / 统计仍直接查该表） */
    void syncReviewSnapshot(String entryType, UUID entryId, String reviewStatus,
                            UUID instanceId, Integer contentVersion, String lastOpinion);

    /** 当前实例的环节常量（便于外部判断） */
    default String currentStageOf(WorkflowTaskResource task) {
        return task == null ? null : task.currentStage();
    }

    /** 环节中文名（透传，避免各层重复 switch） */
    default String stageLabel(String stage) {
        return WorkflowInstanceResource.stageLabel(stage);
    }

    /** 依据环节给出该环节的默认 Camunda Form purpose */
    default String formPurposeOfStage(String stage) {
        if (WorkflowConstants.STAGE_PENDING_INSPECT.equals(stage)) {
            return "inspection";
        }
        if (WorkflowConstants.STAGE_REVISING.equals(stage)) {
            return "revision";
        }
        return "approval";
    }
}
