package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 内容来源（方向 C-1：可溯源）
 *
 * @author cz
 */
@Schema(description = "内容来源 / 参考资料")
public record ContentSourceResource(
        @Schema(description = "来源 ID") String id,
        @Schema(description = "来源名称") String name,
        @Schema(description = "发布机构全称") String publisher,
        @Schema(description = "发布机构简称") String publisherShort,
        @Schema(description = "文档 / 栏目名称") String documentTitle,
        @Schema(description = "原始链接（可能为空）") String url,
        @Schema(description = "来源层级：official / academic / open / other") String sourceType,
        @Schema(description = "来源层级显示名") String sourceTypeLabel,
        @Schema(description = "采集方式：scrape / ocr / manual / api") String collectMethod,
        @Schema(description = "采集方式显示名") String collectMethodLabel,
        @Schema(description = "补充说明") String remark,
        @Schema(description = "该来源在本文内容上的具体说明（仅按内容查询时返回）") String note
) {
}
