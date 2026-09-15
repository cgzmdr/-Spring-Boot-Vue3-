package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 讨论区帖子详情
 */
@Schema(description = "讨论区帖子详情")
public record DiscussionTopicDetailResource(
        @Schema(description = "帖子 ID") String id,
        @Schema(description = "板块 ID") String boardId,
        @Schema(description = "板块名称") String boardName,
        @Schema(description = "标题") String title,
        @Schema(description = "正文") String content,
        @Schema(description = "配图") List<String> images,
        @Schema(description = "作者") DiscussionAuthorResource author,
        @Schema(description = "语言") String lang,
        @Schema(description = "状态：published/pending/hidden/rejected/deleted") String status,
        @Schema(description = "是否置顶") Boolean pinned,
        @Schema(description = "是否精华") Boolean featured,
        @Schema(description = "是否锁定（锁定后不可回复）") Boolean locked,
        @Schema(description = "关联内容类型") String linkedType,
        @Schema(description = "关联内容 ID") String linkedId,
        @Schema(description = "回复数") Integer replyCount,
        @Schema(description = "点赞数") Integer likeCount,
        @Schema(description = "收藏数") Integer favoriteCount,
        @Schema(description = "浏览数") Long viewCount,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "编辑时间") String editedAt,
        @Schema(description = "是否本人发布") Boolean mine,
        @Schema(description = "审核提示（作者可见：待审/驳回原因）") String reviewNote
) {
}
