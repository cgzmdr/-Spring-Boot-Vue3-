package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图片署名核实进度（方向 C-4）
 *
 * <p>如实反映「多少张已核实、多少张还欠着」—— 这既是给读者的交代，
 * 也是给维护者的待办清单规模。</p>
 *
 * @author cz
 */
@Schema(description = "图片署名核实进度")
public record ImageCreditStatsResource(
        @Schema(description = "图片总数") long total,
        @Schema(description = "已核实（有作者与许可）") long verified,
        @Schema(description = "来源待核") long unverified,
        @Schema(description = "原创或无需署名") long original,
        @Schema(description = "需要署名但尚未核实（合规缺口）") long pendingAttribution,
        @Schema(description = "核实完成率（百分比，保留一位小数）") double verifiedRate
) {
}
