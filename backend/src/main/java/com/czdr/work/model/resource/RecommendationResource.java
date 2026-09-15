package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 个性化推荐结果（方向 D）。
 *
 * <p><b>诚实性设计</b>：{@code basis} 与 {@code dataNote} 如实说明本次推荐**实际依据什么**。
 * 站内行为数据量很小（实测全站仅 5 个用户、浏览 23 条、收藏 3 条），
 * 因此绝大多数情况下推荐并非真正的「个性化」，而是内容相似度 + 热度。
 * 与其把结果包装成「为你推荐」造成误导，不如直接告知依据。</p>
 *
 * @param list        推荐条目
 * @param basis       本次推荐的实际依据：personalized 个性化 / interest 兴趣标签 /
 *                    similar 内容相似 / popularity 热度兜底
 * @param basisLabel  依据的中文说明（前台直接展示）
 * @param dataNote    数据说明：如实告知当前可用行为样本量，样本不足时提示
 * @param confidence  画像可信度 0~1（行为越少越低）
 * @param behaviorCount 参与计算的行为条数
 * @param interestCount 用户已选兴趣标签数
 * @author cz
 */
@Schema(description = "个性化推荐结果")
public record RecommendationResource(
        @Schema(description = "推荐条目") List<RecoItem> list,
        @Schema(description = "推荐依据") String basis,
        @Schema(description = "推荐依据说明") String basisLabel,
        @Schema(description = "数据说明（如实告知样本量）") String dataNote,
        @Schema(description = "画像可信度 0~1") double confidence,
        @Schema(description = "参与计算的行为条数") int behaviorCount,
        @Schema(description = "已选兴趣标签数") int interestCount
) {

    /**
     * 推荐条目。
     *
     * @param docType     内容类型
     * @param docId       业务 ID
     * @param url         详情页路由
     * @param title       标题
     * @param summary     摘要
     * @param ethnicName  所属民族
     * @param category    分类
     * @param coverImage  封面
     * @param themeColor  主题色
     * @param score       推荐得分
     * @param reason      推荐理由（如「你关注了藏族」「与你浏览过的 X 相似」「热门内容」）
     */
    @Schema(description = "推荐条目")
    public record RecoItem(
            @Schema(description = "内容类型") String docType,
            @Schema(description = "业务 ID") String docId,
            @Schema(description = "详情页路由") String url,
            @Schema(description = "标题") String title,
            @Schema(description = "摘要") String summary,
            @Schema(description = "所属民族") String ethnicName,
            @Schema(description = "分类") String category,
            @Schema(description = "封面图") String coverImage,
            @Schema(description = "主题色") String themeColor,
            @Schema(description = "推荐得分") double score,
            @Schema(description = "推荐理由") String reason
    ) {
    }
}
