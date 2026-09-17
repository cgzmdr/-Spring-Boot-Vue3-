package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 待办任务 / 审核详情（列表页与详情页共用）。
 *
 * @author cz
 */
@Schema(description = "审批待办任务")
public record WorkflowTaskResource(
        @Schema(description = "工作流实例 ID") UUID instanceId,
        @Schema(description = "内容类型") String entryType,
        @Schema(description = "内容类型中文名") String entryTypeLabel,
        @Schema(description = "内容 ID") UUID entryId,
        @Schema(description = "内容标题") String entryTitle,
        @Schema(description = "内容版本号（第几版）") Integer contentVersion,
        @Schema(description = "实例状态") String status,
        @Schema(description = "当前环节") String currentStage,
        @Schema(description = "当前环节中文名") String currentStageLabel,
        @Schema(description = "当前 Camunda 用户任务 ID") String taskKey,
        @Schema(description = "当前任务名称") String taskName,
        @Schema(description = "Camunda 任务定义 key（审批中/审查中/修改中）") String elementId,
        @Schema(description = "流程定义 ID") String processDefinitionId,
        @Schema(description = "流程定义版本") Integer processDefinitionVersion,
        @Schema(description = "流程实例 ID") String processInstanceKey,
        @Schema(description = "提交人昵称") String submitterName,
        @Schema(description = "提交时间") LocalDateTime startedAt,
        @Schema(description = "最近更新时间") LocalDateTime updatedAt,
        @Schema(description = "当前环节对应的 Camunda Form schema（JSON 原文，可空）") String formSchema,
        @Schema(description = "当前环节对应的表单 formId") String formId,
        @Schema(description = "是否可执行操作（本人是负责人或是管理员）") boolean actionable,
        @Schema(description = "历史意见（按时间正序，含前序所有版本的审批/审查意见）") List<WorkflowOpinionResource> opinions
) {

    public static String entryTypeLabel(String entryType) {
        if (entryType == null) {
            return "";
        }
        return switch (entryType) {
            case "ethnic" -> "民族";
            case "festival" -> "节日";
            case "art" -> "艺术";
            case "topic" -> "专题";
            default -> entryType;
        };
    }
}
