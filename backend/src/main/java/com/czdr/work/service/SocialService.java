package com.czdr.work.service;

import com.czdr.work.model.resource.CommunityUserResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.MentionUserResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 社区社交关系：关注 / 粉丝 / 关注流 / 用户主页
 *
 * @author cr
 */
public interface SocialService {

    /** 关注 / 取消关注（返回关注后的状态） */
    boolean follow(UUID userId, UUID targetId, boolean follow);

    /** 是否已关注 */
    boolean isFollowing(UUID userId, UUID targetId);

    /** 是否互相关注（互关才允许私信） */
    boolean isMutual(UUID userId, UUID targetId);

    /** 用户社区主页（viewerId 可为空） */
    CommunityUserResource profile(String userId, UUID viewerId);

    /** 关注 / 粉丝列表：type = following | followers */
    EasyPageResult<CommunityUserResource> follows(String userId, String type, Pageable pageable);

    /** 关注流：我关注的用户发布的帖子 */
    EasyPageResult<DiscussionTopicBriefResource> followingFeed(UUID userId, Pageable pageable);

    /** 互相关注的用户 ID 列表（供私信选择） */
    java.util.List<String> mutualIds(UUID userId);

    /**
     * @提及联想：按昵称关键字返回候选用户（回复框输入「@」时提示，默认 10 条）。
     * <p>关键字为空时优先返回「我关注的人」，不足部分用信任等级高的活跃用户补齐。</p>
     */
    List<MentionUserResource> suggestUsers(UUID viewerId, String keyword, int limit);
}
