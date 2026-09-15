package com.czdr.work.service;

import com.czdr.work.model.request.DiscussionPostCreateRequest;
import com.czdr.work.model.request.DiscussionTopicCreateRequest;
import com.czdr.work.model.request.DiscussionTopicUpdateRequest;
import com.czdr.work.model.resource.DiscussionBoardResource;
import com.czdr.work.model.resource.DiscussionPostResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.DiscussionTopicDetailResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 讨论区（板块 / 帖子 / 楼层）
 *
 * @author cz
 */
public interface DiscussionService {

    /** 板块列表（前台可见） */
    List<DiscussionBoardResource> boards();

    /** 帖子列表：boardId / keyword / sort / linkedType+linkedId / authorId */
    EasyPageResult<DiscussionTopicBriefResource> topics(String boardId, String keyword, String sort,
                                                        String linkedType, String linkedId, String authorId,
                                                        UUID viewerId, Pageable pageable);

    /** 帖子详情（含审核提示，作者可见自己的待审/驳回内容） */
    DiscussionTopicDetailResource topic(String id, UUID viewerId, boolean admin);

    /** 发帖：返回帖子 ID */
    String createTopic(UUID userId, DiscussionTopicCreateRequest request);

    /** 编辑帖子（作者本人） */
    void updateTopic(UUID userId, String id, DiscussionTopicUpdateRequest request);

    /** 删除帖子（软删；作者或管理员） */
    void deleteTopic(UUID userId, String id, boolean admin);

    /** 楼层列表（1 楼为楼主首帖，由帖子详情返回；此处为 2 楼起） */
    EasyPageResult<DiscussionPostResource> posts(String topicId, UUID viewerId, boolean admin, Pageable pageable);

    /** 回复帖子 */
    DiscussionPostResource createPost(UUID userId, String topicId, DiscussionPostCreateRequest request);

    /** 删除楼层（软删；作者或管理员） */
    void deletePost(UUID userId, String id, boolean admin);

    /** 某内容关联的讨论（民族/节日/艺术/美食详情页底部讨论区） */
    List<DiscussionTopicBriefResource> linkedTopics(String linkedType, String linkedId, int size);

    /** 我的发帖 / 我的回复 */
    EasyPageResult<DiscussionTopicBriefResource> myTopics(UUID userId, Pageable pageable);

    EasyPageResult<DiscussionPostResource> myPosts(UUID userId, Pageable pageable);
}
