package com.czdr.work.service;

import com.czdr.work.model.request.DiscussionHandleRequest;
import com.czdr.work.model.request.DiscussionReviewRequest;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.czdr.work.model.resource.DiscussionReviewItemResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 讨论区治理（后台）：审核队列、举报处理、用户处置、内容处置
 *
 * @author cz
 */
public interface AdminDiscussionService {

    /** 审核队列：targetType = discussion_topic / discussion_post；status = pending / watch / all；sort = oldest 最早优先（默认）/ priority 高优先级优先（block 级敏感词 → 被举报多 → 最早） */
    EasyPageResult<DiscussionReviewItemResource> reviewQueue(String targetType, String status, String sort, Pageable pageable);

    /** 审核结论：approved 通过 / rejected 驳回（同步 content_review、写日志、通知作者） */
    void review(String operatorId, DiscussionReviewRequest request);

    /** 批量审核：返回成功/失败条数（逐条复用单条逻辑，单条失败不影响其余） */
    Map<String, Object> reviewBatch(String operatorId, String targetType, java.util.List<String> ids,
                                    String status, String reason);

    /** 举报工作台 */
    EasyPageResult<DiscussionReportResource> reports(String status, Pageable pageable);

    /** 处理举报：可同时隐藏/删除内容并对作者禁言 */
    void handleReport(String operatorId, String reportId, DiscussionHandleRequest request);

    /** 禁言 / 解除禁言（days <= 0 表示解除） */
    void muteUser(String operatorId, String userId, Integer days, String reason);

    /** 置顶 / 精华 / 锁定 */
    void setFlag(String operatorId, String topicId, String flag, boolean value);

    /** 后台直接隐藏内容（不经过举报流程） */
    void hideContent(String operatorId, String targetType, String targetId, String reason);

    /** 社区数据看板 */
    Map<String, Object> stats();

    /* ---------------------------- 站内公告（OA 群发） ---------------------------- */

    /** 受众人数预览：{ all, active, maxRecipients } */
    Map<String, Object> broadcastAudience();

    /** OA 公告群发：站内通知（type=system）+ 可选邮件；返回 { recipients, emailed, audience }，并写审计日志 */
    Map<String, Object> broadcast(String operatorId, com.czdr.work.model.request.BroadcastRequest request);

    /* ---------------------------- 板块管理 ---------------------------- */

    /** 板块列表（含隐藏板块，后台用） */
    List<Map<String, Object>> boards();

    /** 新增 / 更新板块（按 id 判存；id 为空表示新建） */
    String saveBoard(String operatorId, Map<String, Object> payload);

    /** 板块启用 / 停用 */
    void toggleBoard(String operatorId, String boardId, boolean active);
}
