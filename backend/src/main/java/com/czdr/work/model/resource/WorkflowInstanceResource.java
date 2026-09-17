package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审批工作流实例（一次「提交审批」的全貌）。
 *
 * @author cz
 */
@Schema(description = "审批工作流实例")
public record WorkflowInstanceResource(
        @Schema(description = "实例 ID") UUID id,
        @Schema(description = "内容类型") String entryType,
        @Schema(description = "内容 ID") UUID entryId,
        @Schema(description = "内容标题") String entryTitle,
        @Schema(description = "内容版本号（第几版）") Integer contentVersion,
        @Schema(description = "流程定义 ID") String processDefinitionId,
        @Schema(description = "流程定义版本") Integer processDefinitionVersion,
        @Schema(description = "Camunda 流程实例 ID") String processInstanceKey,
        @Schema(description = "业务键") String businessKey,
        @Schema(description = "实例状态：running / completed / withdrawn / superseded") String status,
        @Schema(description = "实例状态中文名") String statusLabel,
        @Schema(description = "当前环节：pending_review / pending_inspect / revising / published / stopped") String currentStage,
        @Schema(description = "当前环节中文名") String currentStageLabel,
        @Schema(description = "提交人 ID") UUID submitterId,
        @Schema(description = "提交人昵称") String submitterName,
        @Schema(description = "当前待办负责人 ID") UUID currentAssigneeId,
        @Schema(description = "当前 Camunda 用户任务 ID") String currentTaskKey,
        @Schema(description = "当前任务名称") String currentTaskName,
        @Schema(description = "第几轮流转") Integer roundNo,
        @Schema(description = "流程启动时间") LocalDateTime startedAt,
        @Schema(description = "流程结束时间") LocalDateTime finishedAt,
        @Schema(description = "最近一次更新时间") LocalDateTime updatedAt
) {

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "running" -> "进行中";
            case "completed" -> "已完成";
            case "withdrawn" -> "已撤回";
            case "superseded" -> "已被新版取代";
            default -> status;
        };
    }

    public static String stageLabel(String stage) {
        if (stage == null) {
            return "";
        }
        return switch (stage) {
            case "pending_review" -> "待审核员审批";
            case "pending_inspect" -> "待内容管理员审查";
            case "revising" -> "待内容编辑修改";
            case "published" -> "已上线";
            case "stopped" -> "已终止";
            default -> stage;
        };
    }

    public static WorkflowInstanceResource of(com.czdr.work.model.entity.WorkflowInstance i) {
        return new WorkflowInstanceResource(
                i.getId(), i.getEntryType(), i.getEntryId(), i.getEntryTitle(), i.getContentVersion(),
                i.getProcessDefinitionId(), i.getProcessDefinitionVersion(), i.getProcessInstanceKey(),
                i.getBusinessKey(), i.getStatus(), statusLabel(i.getStatus()),
                i.getCurrentStage(), stageLabel(i.getCurrentStage()),
                i.getSubmitterId(), i.getSubmitterName(), i.getCurrentAssigneeId(),
                i.getCurrentTaskKey(), i.getCurrentTaskName(), i.getRoundNo(),
                i.getStartedAt(), i.getFinishedAt(), i.getUpdatedAt());
    }
}
