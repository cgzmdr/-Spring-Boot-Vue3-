package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族列表项（分页列表返回的简略对象）。
 *
 * <p><b>信息量增强</b>：列表原先只带人口/语系/地域三要素，卡片信息偏薄。
 * 这里补充若干「同屏可见、无需再点进详情」的关联指标，
 * 均由库中真实数据聚合而来（不臆造）：</p>
 * <ul>
 *   <li>{@code artCount} —— 关联艺术/非遗条目数（{@code art} 表按 ethnic_group_id 计数）；</li>
 *   <li>{@code festivalCount} —— 关联节日数（{@code festival} 表）；</li>
 *   <li>{@code foodCount} —— 关联美食数（{@code food} 表）；</li>
 *   <li>{@code customCount} —— 关联风俗条目数（{@code ethnic_custom} 表）；</li>
 *   <li>{@code locationCount} —— 聚居地数量（{@code ethnic_location} 表）；</li>
 *   <li>{@code personCount} —— 关联人物数（{@code person_profile} 按民族名匹配）；</li>
 *   <li>{@code autonomousAreaCount} —— 以该民族为自治民族的自治地方数；</li>
 *   <li>{@code populationRank} —— 人口在 56 个民族中的降序排名（1 = 人口最多）；</li>
 * </ul>
 *
 * <p>计数缺失时统一返回 0（而非 null），前端可直接当作「无」处理。</p>
 */
@Schema(description = "民族列表项（分页列表返回的简略对象）")
public record EthnicQueryInfoResource(
        @Schema(description = "民族 ID") String id,
        @Schema(description = "民族名称") String name,
        @Schema(description = "拼音") String pinyin,
        @Schema(description = "人口") long population,
        @Schema(description = "主要聚居地") String[] region,
        @Schema(description = "语系") String languageFamily,
        @Schema(description = "一句话简介") String summary,
        @Schema(description = "封面图") String coverImage,
        @Schema(description = "主题色") String themeColor,

        // ==================== 信息量增强：关联内容计数 ====================
        @Schema(description = "关联艺术/非遗条目数") int artCount,
        @Schema(description = "关联节日数") int festivalCount,
        @Schema(description = "关联美食数") int foodCount,
        @Schema(description = "关联风俗条目数") int customCount,
        @Schema(description = "聚居地数量") int locationCount,
        @Schema(description = "关联人物数") int personCount,
        @Schema(description = "自治地方数量（以该民族为自治民族）") int autonomousAreaCount,
        @Schema(description = "人口排名（1 = 人口最多，基于七普口径）") int populationRank
) {
}
