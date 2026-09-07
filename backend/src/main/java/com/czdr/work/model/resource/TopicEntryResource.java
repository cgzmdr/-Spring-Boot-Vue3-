package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 专题关联条目（多态：民族/节日/艺术）
 *
 * @author cz
 */
@Schema(description = "专题关联条目（多态：民族/节日/艺术）")
public record TopicEntryResource(
        @Schema(description = "条目 ID") String id,
        @Schema(description = "条目类型（ethnic/festival/art）") String entryType,
        @Schema(description = "关联内容 ID") String entryId,
        @Schema(description = "排序号") Integer sortOrder
) {
}
