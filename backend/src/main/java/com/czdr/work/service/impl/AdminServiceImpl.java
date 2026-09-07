package com.czdr.work.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.AesCbcEncryptor;
import com.czdr.work.model.entity.*;
import com.czdr.work.model.entity.proxy.UserAuthProxy;
import com.czdr.work.model.request.AdminUserSaveRequest;
import com.czdr.work.model.resource.ContentStatsResource;
import com.czdr.work.model.resource.StatsOverviewResource;
import com.czdr.work.service.AdminService;
import com.czdr.work.util.RegexValidatorUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    /** 更新时不允许覆盖的字段 */
    private static final Set<String> EXCLUDE_FIELDS =
            Set.of("id", "createdAt", "updatedAt", "createdBy", "updatedBy");

    private final EasyEntityQuery entityQuery;
    private final AesCbcEncryptor aesCbcEncryptor;

    // ==================== 民族 ====================

    @Override
    public void createEthnicGroup(EthnicGroup body) {
        body.setId(UUID.randomUUID());
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("draft");
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    public void updateEthnicGroup(String id, EthnicGroup body) {
        EthnicGroup target = getEthnicGroup(id);
        String oldStatus = target.getStatus();
        copyNonNull(body, target);
        target.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(target).executeRows();
        syncReviewStatus("ethnic", target.getId(), body.getStatus(), oldStatus);
    }

    @Override
    public void deleteEthnicGroup(String id) {
        EthnicGroup target = getEthnicGroup(id);
        entityQuery.deletable(target).allowDeleteStatement(true).executeRows();
        deleteReviewRecord("ethnic", target.getId());
    }

    @Override
    public EasyPageResult<EthnicGroup> listEthnicGroups(String keyword, String status, Pageable pageable) {
        return entityQuery.queryable(EthnicGroup.class)
                .where(e -> {
                    if (status != null && !status.isBlank()) {
                        e.status().eq(status);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        e.name().like(keyword);
                    }
                })
                .orderBy(e -> e.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public EthnicGroup getEthnicGroup(String id) {
        EthnicGroup entity = entityQuery.queryable(EthnicGroup.class)
                .where(e -> e.id().eq(UUID.fromString(id)))
                .include(com.czdr.work.model.entity.proxy.EthnicGroupProxy::customs)
                .include(com.czdr.work.model.entity.proxy.EthnicGroupProxy::locations)
                .include(com.czdr.work.model.entity.proxy.EthnicGroupProxy::foods)
                .include(com.czdr.work.model.entity.proxy.EthnicGroupProxy::festivals)
                .include(com.czdr.work.model.entity.proxy.EthnicGroupProxy::arts)
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    // ==================== 节日 ====================

    @Override
    public void createFestival(Festival body) {
        body.setId(UUID.randomUUID());
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("draft");
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    public void updateFestival(String id, Festival body) {
        Festival target = getFestival(id);
        String oldStatus = target.getStatus();
        copyNonNull(body, target);
        target.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(target).executeRows();
        syncReviewStatus("festival", target.getId(), body.getStatus(), oldStatus);
    }

    @Override
    public void deleteFestival(String id) {
        Festival target = getFestival(id);
        entityQuery.deletable(target).allowDeleteStatement(true).executeRows();
        deleteReviewRecord("festival", target.getId());
    }

    @Override
    public EasyPageResult<Festival> listFestivals(String keyword, String status, Pageable pageable) {
        return entityQuery.queryable(Festival.class)
                .where(f -> {
                    if (status != null && !status.isBlank()) {
                        f.status().eq(status);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        f.name().like(keyword);
                    }
                })
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public Festival getFestival(String id) {
        Festival entity = entityQuery.queryable(Festival.class)
                .where(f -> f.id().eq(UUID.fromString(id)))
                .include(com.czdr.work.model.entity.proxy.FestivalProxy::ethnicGroup)
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    // ==================== 艺术 ====================

    @Override
    public void createArt(Art body) {
        body.setId(UUID.randomUUID());
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("draft");
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    public void updateArt(String id, Art body) {
        Art target = getArt(id);
        String oldStatus = target.getStatus();
        copyNonNull(body, target);
        target.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(target).executeRows();
        syncReviewStatus("art", target.getId(), body.getStatus(), oldStatus);
    }

    @Override
    public void deleteArt(String id) {
        Art target = getArt(id);
        entityQuery.deletable(target).allowDeleteStatement(true).executeRows();
        deleteReviewRecord("art", target.getId());
    }

    @Override
    public EasyPageResult<Art> listArts(String keyword, String status, Pageable pageable) {
        return entityQuery.queryable(Art.class)
                .where(a -> {
                    if (status != null && !status.isBlank()) {
                        a.status().eq(status);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        a.name().like(keyword);
                    }
                })
                .orderBy(a -> a.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public Art getArt(String id) {
        Art entity = entityQuery.queryable(Art.class)
                .where(a -> a.id().eq(UUID.fromString(id)))
                .include(com.czdr.work.model.entity.proxy.ArtProxy::ethnicGroup)
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    // ==================== 专题 ====================

    @Override
    public void createTopic(Topic body) {
        body.setId(UUID.randomUUID());
        if (body.getSlug() == null || body.getSlug().isBlank()) {
            body.setSlug(generateTopicSlug(body.getTitle()));
        }
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("draft");
        }
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    public void updateTopic(String id, Topic body) {
        Topic target = getTopic(id);
        String oldStatus = target.getStatus();
        copyNonNull(body, target);
        if (target.getSlug() == null || target.getSlug().isBlank()) {
            target.setSlug(generateTopicSlug(target.getTitle()));
        }
        target.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(target).executeRows();
        syncReviewStatus("topic", target.getId(), body.getStatus(), oldStatus);
    }

    /** 生成 URL 友好且唯一的专题 slug（中英文标题均可） */
    private String generateTopicSlug(String title) {
        String base = (title == null || title.isBlank()) ? "topic" : title.trim();
        String slug = base.replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase();
        if (slug.isBlank() || slug.length() > 40) {
            slug = "topic";
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void deleteTopic(String id) {
        Topic target = getTopic(id);
        entityQuery.deletable(target).allowDeleteStatement(true).executeRows();
        deleteReviewRecord("topic", target.getId());
    }

    @Override
    public EasyPageResult<Topic> listTopics(String keyword, String status, Pageable pageable) {
        return entityQuery.queryable(Topic.class)
                .where(t -> {
                    if (status != null && !status.isBlank()) {
                        t.status().eq(status);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        t.title().like(keyword);
                    }
                })
                .orderBy(t -> t.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public Topic getTopic(String id) {
        Topic entity = entityQuery.queryable(Topic.class)
                .where(t -> t.id().eq(UUID.fromString(id)))
                .include(com.czdr.work.model.entity.proxy.TopicProxy::entries)
                .firstOrNull();
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    // ==================== 审核 ====================

    @Override
    public EasyPageResult<ContentReview> listReviews(String status, Pageable pageable) {
        // 审核内容单独为一个表    其他实体状态需要同步至该表
        return entityQuery.queryable(ContentReview.class)
                .where(r -> {
                    if (status != null && !status.isBlank()) {
                        r.status().eq(status);
                    }
                })
                .orderBy(r -> r.submittedAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    @Transactional
    public void approveReview(String id) {
        ContentReview review = getReview(id);
        if ("approved".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该内容已审核通过");
        }
        review.setStatus("approved");
        review.setReviewerId(UUID.fromString(StpUtil.getLoginIdAsString()));
        review.setReviewedAt(LocalDateTime.now());
        entityQuery.updatable(review).executeRows();
        updateTargetStatus(review.getEntryType(), review.getEntryId(), "published");
    }

    @Override
    @Transactional
    public void rejectReview(String id, String reason) {
        ContentReview review = getReview(id);
        if ("rejected".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该内容已驳回");
        }
        review.setStatus("rejected");
        review.setReviewerId(UUID.fromString(StpUtil.getLoginIdAsString()));
        review.setRejectReason(reason);
        review.setReviewedAt(LocalDateTime.now());
        entityQuery.updatable(review).executeRows();
        updateTargetStatus(review.getEntryType(), review.getEntryId(), "draft");
    }

    private ContentReview getReview(String id) {
        ContentReview review = entityQuery.queryable(ContentReview.class)
                .where(r -> r.id().eq(UUID.fromString(id)))
                .firstOrNull();
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return review;
    }

    /**
     * 审核通过/驳回后同步目标内容的状态
     */
    private void updateTargetStatus(String entryType, UUID entryId, String status) {
        switch (entryType) {
            case "ethnic" -> {
                EthnicGroup e = entityQuery.queryable(EthnicGroup.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (e != null) {
                    e.setStatus(status);
                    entityQuery.updatable(e).executeRows();
                }
            }
            case "festival" -> {
                Festival f = entityQuery.queryable(Festival.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (f != null) {
                    f.setStatus(status);
                    entityQuery.updatable(f).executeRows();
                }
            }
            case "art" -> {
                Art a = entityQuery.queryable(Art.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (a != null) {
                    a.setStatus(status);
                    entityQuery.updatable(a).executeRows();
                }
            }
            case "topic" -> {
                Topic t = entityQuery.queryable(Topic.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                if (t != null) {
                    t.setStatus(status);
                    entityQuery.updatable(t).executeRows();
                }
            }
            default -> {
            }
        }
    }

    /**
     * 内容状态 -> 审核状态 映射。
     * <p>content_review 表只保留审核流程状态：pending 待审核 / approved 已通过 / rejected 已驳回。
     * 内容状态 draft（草稿）/ rejected（已驳回）统一对应 rejected，published（已发布）对应 approved。</p>
     */
    private String toReviewStatus(String contentStatus) {
        if (contentStatus == null) {
            return null;
        }
        return switch (contentStatus) {
            case "pending" -> "pending";
            case "published" -> "approved";
            case "draft", "rejected" -> "rejected";
            default -> null;
        };
    }

    /**
     * 修改民族/节日/艺术/专题状态后同步审核表（content_review）状态：
     * 已有审核记录则更新状态，不存在则按新状态补齐一条记录；状态字段为空或未变化时跳过。
     */
    private void syncReviewStatus(String entryType, UUID entryId, String newStatus, String oldStatus) {
        if (newStatus == null || newStatus.isBlank() || newStatus.equals(oldStatus)) {
            return;
        }
        String reviewStatus = toReviewStatus(newStatus);
        if (reviewStatus == null) {
            return;
        }
        ContentReview review = entityQuery.queryable(ContentReview.class)
                .where(r -> {
                    r.entryType().eq(entryType);
                    r.entryId().eq(entryId);
                })
                .firstOrNull();
        if (review == null) {
            entityQuery.insertable(new ContentReview(UUID.randomUUID(), entryType, entryId, reviewStatus))
                    .executeRows();
        } else if (!reviewStatus.equals(review.getStatus())) {
            review.setStatus(reviewStatus);
            entityQuery.updatable(review).executeRows();
        }
    }

    /** 删除内容时同步删除其审核记录，避免 content_review 残留孤儿数据 */
    private void deleteReviewRecord(String entryType, UUID entryId) {
        entityQuery.deletable(ContentReview.class)
                .allowDeleteStatement(true)
                .where(r -> {
                    r.entryType().eq(entryType);
                    r.entryId().eq(entryId);
                })
                .executeRows();
    }

    // ==================== 用户 ====================

    @Override
    public EasyPageResult<UserAuth> listUsers(String keyword, Pageable pageable) {
        return entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    if (keyword != null && !keyword.isBlank()) {
                        u.or(() -> {
                            u.account().like(keyword);
                            u.nickname().like(keyword);
                        });
                    }
                })
                .include(UserAuthProxy::roles)
                .orderBy(u -> u.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Override
    public UserAuth getUser(String id) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(UUID.fromString(id)))
                .include(UserAuthProxy::roles)
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    @Override
    @Transactional
    public String createUser(AdminUserSaveRequest request) {
        // 基础校验
        if (isBlank(request.nickname())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
        }
        if (isBlank(request.password())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        RegexValidatorUtil.ValidationResult nicknameCheck = RegexValidatorUtil.validateNickname(request.nickname());
        if (!nicknameCheck.valid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, nicknameCheck.message());
        }
        RegexValidatorUtil.ValidationResult passwordCheck = RegexValidatorUtil.validatePassword(request.password());
        if (!passwordCheck.valid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, passwordCheck.message());
        }
        validateContact(request.mobile(), request.email());

        String account = isBlank(request.account()) ? request.nickname().trim() : request.account().trim();
        checkUnique(account, request.nickname().trim(), request.mobile(), request.email(), null);

        String passwordHash;
        try {
            passwordHash = aesCbcEncryptor.encrypt(request.password());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "密码加密失败");
        }

        UUID userId = UUID.randomUUID();
        UserAuth user = new UserAuth();
        user.setId(userId);
        user.setAccount(account);
        user.setNickname(request.nickname().trim());
        user.setPasswordHash(passwordHash);
        user.setMobile(blankToNull(request.mobile()));
        user.setEmail(blankToNull(request.email()));
        user.setStatus(isBlank(request.status()) ? "active" : request.status().trim());
        if (!"active".equals(user.getStatus()) && !"disabled".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态仅支持 active / disabled");
        }
        user.setLang("zh");
        user.setCreatedAt(LocalDate.now());
        user.setUpdatedAt(LocalDate.now());
        entityQuery.insertable(user).executeRows();

        List<UUID> roleIds = resolveRoleIds(request.roleIds());
        if (roleIds.isEmpty()) {
            // 未指定角色时默认绑定普通用户角色（与注册逻辑一致）
            Role userRole = queryRoleByCode("user");
            if (userRole != null) {
                roleIds = List.of(userRole.getId());
            }
        }
        replaceUserRoles(userId, roleIds);
        return userId.toString();
    }

    @Override
    @Transactional
    public void updateUser(String id, AdminUserSaveRequest request) {
        UUID userId = UUID.fromString(id);
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        String nickname = null;
        String mobile = null;
        String email = null;
        String account = null;
        if (request.nickname() != null) {
            if (request.nickname().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
            }
            RegexValidatorUtil.ValidationResult nicknameCheck = RegexValidatorUtil.validateNickname(request.nickname());
            if (!nicknameCheck.valid()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, nicknameCheck.message());
            }
            nickname = request.nickname().trim();
        }
        if (request.account() != null) {
            if (request.account().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能为空");
            }
            account = request.account().trim();
        }
        if (request.mobile() != null) {
            mobile = blankToNull(request.mobile());
            if (mobile != null) {
                RegexValidatorUtil.ValidationResult mobileCheck = RegexValidatorUtil.validateMobile(mobile);
                if (!mobileCheck.valid()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, mobileCheck.message());
                }
            }
        }
        if (request.email() != null) {
            email = blankToNull(request.email());
            if (email != null) {
                RegexValidatorUtil.ValidationResult emailCheck = RegexValidatorUtil.validateEmail(email);
                if (!emailCheck.valid()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, emailCheck.message());
                }
            }
        }
        checkUnique(account, nickname, mobile, email, userId);

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (account != null) {
            user.setAccount(account);
        }
        if (request.mobile() != null) {
            user.setMobile(mobile);
        }
        if (request.email() != null) {
            user.setEmail(email);
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim();
            if (!"active".equals(status) && !"disabled".equals(status)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "状态仅支持 active / disabled");
            }
            user.setStatus(status);
        }
        if (request.password() != null && !request.password().isBlank()) {
            RegexValidatorUtil.ValidationResult passwordCheck = RegexValidatorUtil.validatePassword(request.password());
            if (!passwordCheck.valid()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, passwordCheck.message());
            }
            try {
                user.setPasswordHash(aesCbcEncryptor.encrypt(request.password()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "密码加密失败");
            }
        }
        user.setUpdatedAt(LocalDate.now());
        entityQuery.updatable(user).executeRows();

        // 仅当提交了 roleIds 字段时调整角色（null 表示不修改）
        if (request.roleIds() != null) {
            replaceUserRoles(userId, resolveRoleIds(request.roleIds()));
        }
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        UUID userId = UUID.fromString(id);
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (userId.toString().equals(StpUtil.getLoginIdAsString())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能删除当前登录账号");
        }
        // 清理关联数据（表间无外键约束，需手动清理，避免残留孤儿数据）
        entityQuery.deletable(UserRole.class)
                .allowDeleteStatement(true)
                .where(ur -> ur.userId().eq(userId))
                .executeRows();
        entityQuery.deletable(Favorite.class)
                .allowDeleteStatement(true)
                .where(f -> f.userId().eq(userId))
                .executeRows();
        entityQuery.deletable(LikeRecord.class)
                .allowDeleteStatement(true)
                .where(l -> l.userId().eq(userId))
                .executeRows();
        entityQuery.deletable(user).allowDeleteStatement(true).executeRows();
    }

    @Override
    @Transactional
    public void assignUserRoles(String id, List<UUID> roleIds) {
        UUID userId = UUID.fromString(id);
        if (entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId)).firstOrNull() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        replaceUserRoles(userId, roleIds);
    }

    /** 校验手机号 / 邮箱格式（任一为空则跳过） */
    private void validateContact(String mobile, String email) {
        if (mobile != null && !mobile.isBlank()) {
            RegexValidatorUtil.ValidationResult mobileCheck = RegexValidatorUtil.validateMobile(mobile.trim());
            if (!mobileCheck.valid()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, mobileCheck.message());
            }
        }
        if (email != null && !email.isBlank()) {
            RegexValidatorUtil.ValidationResult emailCheck = RegexValidatorUtil.validateEmail(email.trim());
            if (!emailCheck.valid()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, emailCheck.message());
            }
        }
    }

    /**
     * 校验登录标识（账号/昵称/手机号/邮箱）是否与其他用户冲突。
     * 为空的值跳过；excludeId 非空时排除该用户自身（编辑场景）。
     */
    private void checkUnique(String account, String nickname, String mobile, String email, UUID excludeId) {
        checkUniqueField("account", account, excludeId);
        checkUniqueField("nickname", nickname, excludeId);
        checkUniqueField("mobile", mobile, excludeId);
        checkUniqueField("email", email, excludeId);
    }

    private void checkUniqueField(String field, String value, UUID excludeId) {
        if (value == null || value.isBlank()) {
            return;
        }
        boolean exists = switch (field) {
            case "account" -> entityQuery.queryable(UserAuth.class)
                    .where(u -> {
                        u.account().eq(value);
                        if (excludeId != null) {
                            u.id().ne(excludeId);
                        }
                    }).firstOrNull() != null;
            case "nickname" -> entityQuery.queryable(UserAuth.class)
                    .where(u -> {
                        u.nickname().eq(value);
                        if (excludeId != null) {
                            u.id().ne(excludeId);
                        }
                    }).firstOrNull() != null;
            case "mobile" -> entityQuery.queryable(UserAuth.class)
                    .where(u -> {
                        u.mobile().eq(value);
                        if (excludeId != null) {
                            u.id().ne(excludeId);
                        }
                    }).firstOrNull() != null;
            case "email" -> entityQuery.queryable(UserAuth.class)
                    .where(u -> {
                        u.email().eq(value);
                        if (excludeId != null) {
                            u.id().ne(excludeId);
                        }
                    }).firstOrNull() != null;
            default -> false;
        };
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATE, "该" + fieldLabel(field) + "已被占用: " + value);
        }
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "account" -> "账号";
            case "nickname" -> "昵称";
            case "mobile" -> "手机号";
            case "email" -> "邮箱";
            default -> field;
        };
    }

    /** 字符串角色 ID 列表 -> UUID 列表（非法值直接报参数错误） */
    private List<UUID> resolveRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>(roleIds.size());
        for (String roleId : roleIds) {
            if (roleId == null || roleId.isBlank()) {
                continue;
            }
            try {
                result.add(UUID.fromString(roleId.trim()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "角色 ID 格式错误: " + roleId);
            }
        }
        return result;
    }

    /**
     * 全量替换用户角色绑定：先校验角色存在，再删除旧绑定并插入新绑定。
     */
    private void replaceUserRoles(UUID userId, List<UUID> roleIds) {
        for (UUID roleId : roleIds) {
            if (entityQuery.queryable(Role.class)
                    .where(r -> r.id().eq(roleId)).firstOrNull() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不存在: " + roleId);
            }
        }
        entityQuery.deletable(UserRole.class)
                .allowDeleteStatement(true)
                .where(ur -> ur.userId().eq(userId))
                .executeRows();
        for (UUID roleId : roleIds) {
            entityQuery.insertable(new UserRole(userId, roleId)).executeRows();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Role queryRoleByCode(String code) {
        return entityQuery.queryable(Role.class)
                .where(r -> r.code().eq(code))
                .firstOrNull();
    }

    // ==================== 角色 ====================

    @Override
    public List<Role> listRoles() {
        return entityQuery.queryable(Role.class)
                .include(com.czdr.work.model.entity.proxy.RoleProxy::permissions)
                .orderBy(r -> r.createdAt().desc())
                .toList();
    }

    @Override
    public void createRole(Role body) {
        boolean exists = entityQuery.queryable(Role.class)
                .where(r -> r.code().eq(body.getCode())).firstOrNull() != null;
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATE, "角色编码已存在: " + body.getCode());
        }
        body.setId(UUID.randomUUID());
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
    }

    @Override
    @Transactional
    public void assignRolePermissions(String id, List<UUID> permissionIds) {
        UUID roleId = UUID.fromString(id);
        if (entityQuery.queryable(Role.class)
                .where(r -> r.id().eq(roleId)).firstOrNull() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        for (UUID permissionId : permissionIds) {
            if (entityQuery.queryable(Permission.class)
                    .where(p -> p.id().eq(permissionId)).firstOrNull() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "权限不存在: " + permissionId);
            }
        }
        entityQuery.deletable(RolePermission.class)
                .allowDeleteStatement(true)
                .where(rp -> rp.roleId().eq(roleId))
                .executeRows();
        for (UUID permissionId : permissionIds) {
            entityQuery.insertable(new RolePermission(roleId, permissionId)).executeRows();
        }
    }

    @Override
    public List<Permission> listPermissions() {
        return entityQuery.queryable(Permission.class)
                .orderBy(p -> p.createdAt().asc())
                .toList();
    }

    // ==================== 反馈 ====================

    @Override
    public EasyPageResult<Feedback> listFeedback(String keyword, Pageable pageable) {
        return entityQuery.queryable(Feedback.class)
                .where(f -> {
                    if (keyword != null && !keyword.isBlank()) {
                        f.or(() -> {
                            f.name().like(keyword);
                            f.contact().like(keyword);
                            f.content().like(keyword);
                        });
                    }
                })
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
    }

    // ==================== 浏览量 ====================

    @Override
    public Map<String, Long> getViewCounts(String type, List<String> ids) {
        if (type == null || type.isBlank() || ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<UUID> entryIds = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                entryIds.add(UUID.fromString(id.trim()));
            } catch (IllegalArgumentException ignored) {
                // 跳过非法 id
            }
        }
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> result = new HashMap<>();
        entityQuery.queryable(ViewCounter.class)
                .where(v -> {
                    v.entryType().eq(type);
                    v.entryId().in(entryIds);
                })
                .toList()
                .forEach(c -> result.put(c.getEntryId().toString(), c.getCount()));
        return result;
    }

    // ==================== 统计 ====================

    @Override
    public StatsOverviewResource statsOverview() {
        long users = entityQuery.queryable(UserAuth.class).count();
        long ethnic = entityQuery.queryable(EthnicGroup.class).count();
        long festival = entityQuery.queryable(Festival.class).count();
        long art = entityQuery.queryable(Art.class).count();
        long topic = entityQuery.queryable(Topic.class).count();
        long pendingReviews = entityQuery.queryable(ContentReview.class)
                .where(r -> r.status().eq("pending")).count();
        return new StatsOverviewResource(users, ethnic, festival, art, topic, pendingReviews);
    }

    @Override
    public List<ContentStatsResource> statsContent() {
        return List.of(
                ethnicStats(),
                festivalStats(),
                artStats(),
                topicStats()
        );
    }

    /**
     * 全量重建审核记录（同步审核信息）：
     * 清空 content_review 后按当前民族/节日/艺术/专题内容重新生成，
     * 审核状态与内容状态保持一致映射，同时清理历史脏状态与孤儿记录。
     */
    @Override
    @Transactional
    public void initReview() {
        // easy-query 禁止无 WHERE 的全表删除，按现有条目类型范围清空后全量重建
        entityQuery.deletable(ContentReview.class)
                .allowDeleteStatement(true)
                .where(r -> r.entryType().in(List.of("art", "topic", "festival", "ethnic")))
                .executeRows();

        entityQuery.queryable(Art.class).toList()
                .forEach(art -> insertReview("art", art.getId(), art.getStatus()));
        entityQuery.queryable(Topic.class).toList()
                .forEach(topic -> insertReview("topic", topic.getId(), topic.getStatus()));
        entityQuery.queryable(Festival.class).toList()
                .forEach(festival -> insertReview("festival", festival.getId(), festival.getStatus()));
        entityQuery.queryable(EthnicGroup.class).toList()
                .forEach(ethnicGroup -> insertReview("ethnic", ethnicGroup.getId(), ethnicGroup.getStatus()));
    }

    /** 按内容状态插入一条审核记录（状态无映射时跳过） */
    private void insertReview(String entryType, UUID entryId, String contentStatus) {
        String reviewStatus = toReviewStatus(contentStatus);
        if (reviewStatus == null) {
            return;
        }
        entityQuery.insertable(new ContentReview(UUID.randomUUID(), entryType, entryId, reviewStatus))
                .executeRows();
    }

    private ContentStatsResource ethnicStats() {
        return new ContentStatsResource("ethnic",
                entityQuery.queryable(EthnicGroup.class).count(),
                entityQuery.queryable(EthnicGroup.class).where(e -> e.status().eq("published")).count(),
                entityQuery.queryable(EthnicGroup.class).where(e -> e.status().eq("draft")).count(),
                entityQuery.queryable(EthnicGroup.class).where(e -> e.status().eq("pending")).count(),
                entityQuery.queryable(EthnicGroup.class).where(e -> e.status().eq("offline")).count());
    }

    private ContentStatsResource festivalStats() {
        return new ContentStatsResource("festival",
                entityQuery.queryable(Festival.class).count(),
                entityQuery.queryable(Festival.class).where(e -> e.status().eq("published")).count(),
                entityQuery.queryable(Festival.class).where(e -> e.status().eq("draft")).count(),
                entityQuery.queryable(Festival.class).where(e -> e.status().eq("pending")).count(),
                entityQuery.queryable(Festival.class).where(e -> e.status().eq("offline")).count());
    }

    private ContentStatsResource artStats() {
        return new ContentStatsResource("art",
                entityQuery.queryable(Art.class).count(),
                entityQuery.queryable(Art.class).where(e -> e.status().eq("published")).count(),
                entityQuery.queryable(Art.class).where(e -> e.status().eq("draft")).count(),
                entityQuery.queryable(Art.class).where(e -> e.status().eq("pending")).count(),
                entityQuery.queryable(Art.class).where(e -> e.status().eq("offline")).count());
    }

    private ContentStatsResource topicStats() {
        return new ContentStatsResource("topic",
                entityQuery.queryable(Topic.class).count(),
                entityQuery.queryable(Topic.class).where(e -> e.status().eq("published")).count(),
                entityQuery.queryable(Topic.class).where(e -> e.status().eq("draft")).count(),
                entityQuery.queryable(Topic.class).where(e -> e.status().eq("pending")).count(),
                entityQuery.queryable(Topic.class).where(e -> e.status().eq("offline")).count());
    }

    /**
     * 将请求体中非 null 字段拷贝到目标实体（更新时保留其余字段）
     */
    private void copyNonNull(Object source, Object target) {
        for (Field field : source.getClass().getDeclaredFields()) {
            if (EXCLUDE_FIELDS.contains(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (value != null) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException ignored) {
                // 忽略无法访问的字段
            }
        }
    }
}
