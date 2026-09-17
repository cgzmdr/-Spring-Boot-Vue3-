package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 内容审批全过程（详情页用）：实例时间线 + 逐版本意见 + 当前环节。
 *
 * @author cz
 */
@Schema(description = "内容审批全过程")
public record WorkflowTimelineResource(
        @Schema(description = "内容类型") String entryType,
        @Schema(description = "内容 ID") UUID entryId,
        @Schema(description = "内容标题") String entryTitle,
        @Schema(description = "当前内容状态：draft/pending/published/offline/rejected") String contentStatus,
        @Schema(description = "当前内容版本号") Integer contentVersion,
        @Schema(description = "历次工作流实例（按版本倒序）") List<WorkflowInstanceResource> instances,
        @Schema(description = "全部意见（按时间正序，可直接用于展示「前面的审批意见和审查意见」）") List<WorkflowOpinionResource> opinions,
        @Schema(description = "待办中的工作流实例（可空）") WorkflowInstanceResource activeInstance,
        @Schema(description = "查询时间") LocalDateTime queriedAt
) {
}
