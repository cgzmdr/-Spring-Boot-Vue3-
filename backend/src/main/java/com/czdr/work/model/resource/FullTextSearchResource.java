package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 全文检索结果（方向 D）。
 *
 * <p>与改造前的 {@link SearchResultResource}（民族/节日/艺术三组固定结构）不同，
 * 这里改为**统一结果流**：所有内容类型混排并按相关度排序，另给分面计数供筛选。
 * 用户搜「藏族」时能一次看到民族、节日、艺术、美食、人物、自治地方、体育等全部相关内容，
 * 而不只是前三类。</p>
 *
 * <p>{@link SearchResultResource} 保留不动，供旧接口与既有前端平滑过渡。</p>
 *
 * @author cz
 */
@Schema(description = "全文检索结果（统一结果流）")
public record FullTextSearchResource(
        @Schema(description = "检索词") String keyword,
        @Schema(description = "命中总数") long total,
        @Schema(description = "检索耗时（毫秒）") long tookMs,
        @Schema(description = "类型筛选（all 不限）") String type,
        @Schema(description = "结果列表（按相关度排序）") List<SearchHit> list,
        @Schema(description = "分面计数：内容类型 → 命中数") Map<String, Long> facets,
        @Schema(description = "命中来源分布：title/pinyin/abbr/body → 条数") Map<String, Long> highlights
) {

    /**
     * 单条命中。
     *
     * @param docType     内容类型
     * @param docId       业务 ID
     * @param url         详情页路由
     * @param title       标题（纯文本）
     * @param titleHtml   标题（关键词以 &lt;em class="hl"&gt; 包裹）
     * @param summary     摘要（纯文本）
     * @param summaryHtml 摘要（关键词高亮）
     * @param ethnicName  所属民族
     * @param category    分类
     * @param region      地区
     * @param coverImage  封面图
     * @param themeColor  主题色
     * @param score       相关度得分（便于后台调试排序依据）
     * @param matchBy     命中方式：title / pinyin / abbr / body
     */
    @Schema(description = "检索命中项")
    public record SearchHit(
            @Schema(description = "内容类型") String docType,
            @Schema(description = "业务 ID") String docId,
            @Schema(description = "详情页路由") String url,
            @Schema(description = "标题") String title,
            @Schema(description = "标题（高亮 HTML）") String titleHtml,
            @Schema(description = "摘要") String summary,
            @Schema(description = "摘要（高亮 HTML）") String summaryHtml,
            @Schema(description = "所属民族") String ethnicName,
            @Schema(description = "分类") String category,
            @Schema(description = "地区") String region,
            @Schema(description = "封面图") String coverImage,
            @Schema(description = "主题色") String themeColor,
            @Schema(description = "相关度得分") double score,
            @Schema(description = "命中方式") String matchBy
    ) {
    }
}
