package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 讨论区楼层
 */
@Schema(description = "讨论区楼层")
public record DiscussionPostResource(
        @Schema(description = "楼层 ID") String id,
        @Schema(description = "所属帖子 ID") String topicId,
        @Schema(description = "楼层号（1 楼为楼主首帖）") Integer floorNo,
        @Schema(description = "正文") String content,
        @Schema(description = "配图") List<String> images,
        @Schema(description = "作者") DiscussionAuthorResource author,
        @Schema(description = "语言") String lang,
        @Schema(description = "状态") String status,
        @Schema(description = "点赞数") Integer likeCount,
        @Schema(description = "引用的楼层 ID") String quotePostId,
        @Schema(description = "引用楼层摘要") String quoteExcerpt,
        @Schema(description = "引用楼层作者昵称") String quoteAuthor,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "是否本人发布") Boolean mine
) {
}
