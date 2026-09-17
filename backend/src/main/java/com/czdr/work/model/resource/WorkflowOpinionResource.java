package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 审批 / 审查意见（工作流意见流水的一条）。
 *
 * @author cz
 */
@Schema(description = "审批/审查意见")
public record WorkflowOpinionResource(
        @Schema(description = "意见 ID") UUID id,
        @Schema(description = "工作流实例 ID") UUID instanceId,
        @Schema(description = "内容类型") String entryType,
        @Schema(description = "内容 ID") UUID entryId,
        @Schema(description = "内容版本号") Integer contentVersion,
        @Schema(description = "环节：submit 提交 / approve 审批 / inspect 审查 / offline 下线 / revise 修改 / publish 上线") String stage,
        @Schema(description = "环节中文名") String stageLabel,
        @Schema(description = "结论：submitted / approved / rejected / issue / resolved") String decision,
        @Schema(description = "结论中文名") String decisionLabel,
        @Schema(description = "意见正文") String opinion,
        @Schema(description = "操作人 ID") UUID operatorId,
        @Schema(description = "操作人昵称") String operatorName,
        @Schema(description = "操作人角色：reviewer / content_admin / editor") String operatorRole,
        @Schema(description = "Camunda 用户任务 ID") String camundaTaskKey,
        @Schema(description = "任务名称") String taskName,
        @Schema(description = "发生时间") LocalDateTime createdAt
) {

    /** 环节中文名 */
    public static String stageLabel(String stage) {
        if (stage == null) {
            return "";
        }
        return switch (stage) {
            case "submit" -> "提交审批";
            case "approve" -> "审核员审批";
            case "inspect" -> "内容管理员审查";
            case "offline" -> "暂时下线";
            case "revise" -> "内容编辑修改";
            case "publish" -> "重新上线";
            default -> stage;
        };
    }

    /** 结论中文名 */
    public static String decisionLabel(String decision) {
        if (decision == null) {
            return "";
        }
        return switch (decision) {
            case "submitted" -> "已提交";
            case "approved" -> "通过";
            case "rejected" -> "退回";
            case "issue" -> "发现问题";
            case "resolved" -> "已修正";
            default -> decision;
        };
    }

    public static WorkflowOpinionResource of(com.czdr.work.model.entity.WorkflowOpinion o) {
        return new WorkflowOpinionResource(
                o.getId(), o.getInstanceId(), o.getEntryType(), o.getEntryId(), o.getContentVersion(),
                o.getStage(), stageLabel(o.getStage()),
                o.getDecision(), decisionLabel(o.getDecision()),
                o.getOpinion(), o.getOperatorId(), o.getOperatorName(), o.getOperatorRole(),
                o.getCamundaTaskKey(), o.getTaskName(), o.getCreatedAt());
    }

    public static List<WorkflowOpinionResource> of(List<com.czdr.work.model.entity.WorkflowOpinion> list) {
        return list.stream().map(WorkflowOpinionResource::of).toList();
    }
}
