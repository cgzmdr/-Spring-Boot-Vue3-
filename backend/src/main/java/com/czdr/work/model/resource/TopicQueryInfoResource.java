package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 专题对外模型
 *
 * @author cz
 */
@Schema(description = "专题对外模型")
public record TopicQueryInfoResource(
        @Schema(description = "专题 ID") String id,
        @Schema(description = "URL 标识") String slug,
        @Schema(description = "标题") String title,
        @Schema(description = "副标题") String subtitle,
        @Schema(description = "简介") String description,
        @Schema(description = "封面图") String coverImage,
        @Schema(description = "排序号") Integer orderNum,
        @Schema(description = "关联条目列表") List<TopicEntryResource> entries
) {
}
