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

    // 审核
    EasyPageResult<ContentReview> listReviews(String status, Pageable pageable);

    void approveReview(String id);

    void rejectReview(String id, String reason);

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

    void initReview();
}
