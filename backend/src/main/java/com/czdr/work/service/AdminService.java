package com.czdr.work.service;

import com.czdr.work.model.entity.*;
import com.czdr.work.model.request.AdminUserSaveRequest;
import com.czdr.work.model.resource.ContentStatsResource;
import com.czdr.work.model.resource.StatsOverviewResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 后台管理（内容 CRUD / 审核 / 用户 / 角色 / 统计）
 *
 * @author cz
 */
public interface AdminService {
    // 民族
    void createEthnicGroup(EthnicGroup body);

    void updateEthnicGroup(String id, EthnicGroup body);

    void deleteEthnicGroup(String id);

    EasyPageResult<EthnicGroup> listEthnicGroups(String keyword, String status, Pageable pageable);

    EthnicGroup getEthnicGroup(String id);

    // 节日
    void createFestival(Festival body);

    void updateFestival(String id, Festival body);

    void deleteFestival(String id);

    EasyPageResult<Festival> listFestivals(String keyword, String status, Pageable pageable);

    Festival getFestival(String id);

    // 艺术
    void createArt(Art body);

    void updateArt(String id, Art body);

    void deleteArt(String id);

    EasyPageResult<Art> listArts(String keyword, String status, Pageable pageable);

    Art getArt(String id);

    // 专题
    void createTopic(Topic body);

    void updateTopic(String id, Topic body);

    void deleteTopic(String id);

    EasyPageResult<Topic> listTopics(String keyword, String status, Pageable pageable);

    Topic getTopic(String id);

    // 审核（内容审批工作流，对齐 Camunda 8）
    EasyPageResult<ContentReview> listReviews(String status, String entryType, Pageable pageable);

    /**
     * 审核员审批通过：上线并流转到内容管理员审查。
     *
     * @param id      工作流实例 ID（兼容旧数据时也可传 content_review 记录 ID）
     * @param opinion 审批意见（必填）
     */
    void approveReview(String id, String opinion);

    /**
     * 审核员退回：交内容编辑修改。
     */
    void rejectReview(String id, String reason);

    /**
     * 内容管理员审查通过（内容保持在线，本轮闭环结束）。
     */
    void inspectPass(String id, String opinion);

    /**
     * 内容管理员审查发现问题：内容暂时下线并交内容编辑修改。
     */
    void inspectIssue(String id, String reason);

    /**
     * 内容编辑修改完成，重新提交审核员二次审批。
     */
    void reviseAndResubmit(String id, String revisionNote, boolean needReapproval);

    // 用户
    EasyPageResult<UserAuth> listUsers(String keyword, Pageable pageable);

    UserAuth getUser(String id);

    String createUser(AdminUserSaveRequest request);

    void updateUser(String id, AdminUserSaveRequest request);

    void deleteUser(String id);

    void assignUserRoles(String id, List<UUID> roleIds);

    // 角色
    List<Role> listRoles();

    void createRole(Role body);

    void assignRolePermissions(String id, List<UUID> permissionIds);

    // 权限
    List<Permission> listPermissions();

    // 反馈
    EasyPageResult<Feedback> listFeedback(String keyword, Pageable pageable);

    // 浏览量
    Map<String, Long> getViewCounts(String type, List<String> ids);

    // 统计
    StatsOverviewResource statsOverview();

    List<ContentStatsResource> statsContent();

    /**
     * 全量重建审核记录（对齐工作流实例状态）。
     * <p>已废弃旧的「按内容 status 直接映射」逻辑，改为按 workflow_instance 的真实环节回填，
     * 避免出现「内容已下线但审核表显示已通过」这类不一致。</p>
     */
    void initReview();
}
