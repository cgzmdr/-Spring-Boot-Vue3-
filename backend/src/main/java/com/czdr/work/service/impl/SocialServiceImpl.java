package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.DiscussionBlock;
import com.czdr.work.model.entity.DiscussionFollow;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.resource.CommunityUserResource;
import com.czdr.work.model.resource.DiscussionAuthorResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.MentionUserResource;
import com.czdr.work.service.NotificationService;
import com.czdr.work.service.SocialService;
import com.czdr.work.util.MentionParser;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 关注 / 粉丝 / 关注流实现。
 * 关注流采用「拉模式」：按我关注的人查帖子（粉丝量级上升后可切换推模式收件箱）。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** @提及联想：默认条数与上限（前端输入「@」时提示约 10 个候选） */
    private static final int DEFAULT_SUGGEST_SIZE = 10;
    private static final int MAX_SUGGEST_SIZE = 20;
    /** 昵称最大长度，与 MentionParser 的解析上限保持一致 */
    private static final int MAX_MENTION_NAME = 32;

    private final EasyEntityQuery entityQuery;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public boolean follow(UUID userId, UUID targetId, boolean follow) {
        if (userId == null || targetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户参数不正确");
        }
        if (userId.equals(targetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能关注自己");
        }
        UserAuth target = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(targetId))
                .firstOrNull();
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (follow && blockedBetween(userId, targetId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你们之间存在拉黑关系，无法关注");
        }
        DiscussionFollow existing = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> {
                    f.userId().eq(userId);
                    f.followUserId().eq(targetId);
                })
                .firstOrNull();
        if (follow) {
            if (existing != null) {
                return true;
            }
            DiscussionFollow entity = new DiscussionFollow();
            entity.setId(UUID.randomUUID());
            entity.setUserId(userId);
            entity.setFollowUserId(targetId);
            entity.setCreatedAt(LocalDateTime.now());
            entityQuery.insertable(entity).executeRows();
            notificationService.notify(targetId, "follow", userId, "user", userId,
                    "有人关注了你", "%s 关注了你，去看看 TA 的主页吧。".formatted(nicknameOf(userId)), null);
            return true;
        }
        if (existing != null) {
            // EasyQuery 删除需显式允许删除语句，否则静默不执行
            entityQuery.deletable(existing).allowDeleteStatement(true).executeRows();
        }
        return false;
    }

    @Override
    public boolean isFollowing(UUID userId, UUID targetId) {
        if (userId == null || targetId == null) {
            return false;
        }
        return entityQuery.queryable(DiscussionFollow.class)
                .where(f -> {
                    f.userId().eq(userId);
                    f.followUserId().eq(targetId);
                })
                .firstOrNull() != null;
    }

    @Override
    public boolean isMutual(UUID userId, UUID targetId) {
        return userId != null && targetId != null && !userId.equals(targetId)
                && isFollowing(userId, targetId) && isFollowing(targetId, userId);
    }

    @Override
    public CommunityUserResource profile(String userId, UUID viewerId) {
        UUID id = parseUuid(userId);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户 ID 不正确");
        }
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(id))
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        long following = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> f.userId().eq(id))
                .toPageResult(1, 1)
                .getTotal();
        long followers = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> f.followUserId().eq(id))
                .toPageResult(1, 1)
                .getTotal();
        long topics = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.authorId().eq(id);
                    t.status().eq("published");
                })
                .toPageResult(1, 1)
                .getTotal();
        boolean self = viewerId != null && viewerId.equals(id);
        return new CommunityUserResource(
                user.getId().toString(),
                nickname(user),
                user.getAvatar(),
                user.getBio(),
                user.getLocale() == null ? user.getLang() : user.getLocale(),
                user.getTrustLevel() == null ? 0 : user.getTrustLevel(),
                formatDate(user.getCreatedAt()),
                following,
                followers,
                topics,
                viewerId != null && isFollowing(viewerId, id),
                isMutual(viewerId, id),
                self
        );
    }

    @Override
    public EasyPageResult<CommunityUserResource> follows(String userId, String type, Pageable pageable) {
        UUID id = parseUuid(userId);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户 ID 不正确");
        }
        boolean followers = "followers".equalsIgnoreCase(type);
        EasyPageResult<DiscussionFollow> page = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> {
                    if (followers) {
                        f.followUserId().eq(id);
                    } else {
                        f.userId().eq(id);
                    }
                })
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        List<UUID> ids = page.getData().stream()
                .map(f -> followers ? f.getUserId() : f.getFollowUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, UserAuth> users = loadUsers(ids);
        List<CommunityUserResource> data = new ArrayList<>();
        for (UUID uid : ids) {
            UserAuth user = users.get(uid);
            if (user == null) {
                continue;
            }
            data.add(new CommunityUserResource(
                    user.getId().toString(),
                    nickname(user),
                    user.getAvatar(),
                    user.getBio(),
                    user.getLocale() == null ? user.getLang() : user.getLocale(),
                    user.getTrustLevel() == null ? 0 : user.getTrustLevel(),
                    formatDate(user.getCreatedAt()),
                    0L,
                    0L,
                    0L,
                    false,
                    false,
                    false
            ));
        }
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    @Override
    public EasyPageResult<DiscussionTopicBriefResource> followingFeed(UUID userId, Pageable pageable) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<UUID> following = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> f.userId().eq(userId))
                .toList()
                .stream()
                .map(DiscussionFollow::getFollowUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (following.isEmpty()) {
            return new DefaultPageResult<>(0L, List.of());
        }
        EasyPageResult<DiscussionTopic> page = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.authorId().in(following);
                    t.status().eq("published");
                })
                .orderBy(t -> t.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        List<DiscussionTopic> rows = page.getData();
        Map<UUID, UserAuth> users = loadUsers(rows.stream().map(DiscussionTopic::getAuthorId).distinct().toList());
        List<DiscussionTopicBriefResource> data = rows.stream()
                .map(t -> {
                    UserAuth author = users.get(t.getAuthorId());
                    return new DiscussionTopicBriefResource(
                            t.getId().toString(),
                            t.getBoardId().toString(),
                            null,
                            t.getTitle(),
                            excerpt(t.getContent(), 120),
                            parseImages(t.getImages()),
                            new DiscussionAuthorResource(
                                    t.getAuthorId().toString(), nickname(author), author == null ? null : author.getAvatar(),
                                    author == null || author.getTrustLevel() == null ? 0 : author.getTrustLevel(), false),
                            t.getLang(),
                            t.getStatus(),
                            t.getPinned(),
                            t.getFeatured(),
                            t.getLocked(),
                            t.getReplyCount(),
                            t.getLikeCount(),
                            t.getViewCount(),
                            format(t.getLastReplyAt()),
                            format(t.getCreatedAt()),
                            false
                    );
                })
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    @Override
    public List<String> mutualIds(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        List<UUID> following = entityQuery.queryable(DiscussionFollow.class)
                .where(f -> f.userId().eq(userId))
                .toList()
                .stream()
                .map(DiscussionFollow::getFollowUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (following.isEmpty()) {
            return List.of();
        }
        return entityQuery.queryable(DiscussionFollow.class)
                .where(f -> {
                    f.userId().in(following);
                    f.followUserId().eq(userId);
                })
                .toList()
                .stream()
                .map(f -> f.getUserId().toString())
                .distinct()
                .toList();
    }

    @Override
    public List<MentionUserResource> suggestUsers(UUID viewerId, String keyword, int limit) {
        int size = limit <= 0 ? DEFAULT_SUGGEST_SIZE : Math.min(limit, MAX_SUGGEST_SIZE);
        String kw = normalizeKeyword(keyword);
        // 同一用户只出现一次；无关键字时「我关注的人」优先占位
        Map<UUID, UserAuth> picked = new LinkedHashMap<>();
        if (kw.isEmpty()) {
            List<UUID> following = followingIds(viewerId, size);
            Map<UUID, UserAuth> followed = loadUsers(following);
            for (UUID id : following) {
                UserAuth user = followed.get(id);
                if (isSuggestable(user, viewerId)) {
                    picked.putIfAbsent(id, user);
                }
            }
        }
        if (picked.size() < size) {
            for (UserAuth user : searchableUsers(viewerId, kw, size)) {
                if (isSuggestable(user, viewerId)) {
                    picked.putIfAbsent(user.getId(), user);
                }
            }
        }
        return picked.values().stream()
                // 昵称含空格 / emoji 等字符的用户无法被 `@昵称` 正确解析，不参与联想
                .filter(u -> MentionParser.isMentionable(u.getNickname()))
                .limit(size)
                .map(u -> new MentionUserResource(u.getId().toString(), nickname(u), compactAvatar(u), u.getBio()))
                .toList();
    }

    /** 双方之间是否存在任意方向的拉黑（拉黑期间禁止关注，也不会有互关，因此无法私信） */
    private boolean blockedBetween(UUID userId, UUID targetId) {
        return entityQuery.queryable(DiscussionBlock.class)
                .where(b -> {
                    b.userId().eq(userId);
                    b.blockedUserId().eq(targetId);
                })
                .firstOrNull() != null
                || entityQuery.queryable(DiscussionBlock.class)
                .where(b -> {
                    b.userId().eq(targetId);
                    b.blockedUserId().eq(userId);
                })
                .firstOrNull() != null;
    }

    // ---------------------------------------------------------------- 工具

    /**
     * @提及联想：归一化关键字 —— 去掉空白与 LIKE 通配符（% _），并按昵称最大长度截断。
     * 关键字为空（或只剩通配符）时，联想接口退化为「推荐用户」。
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String kw = keyword.replace("%", "").replace("_", "").trim();
        return kw.length() > MAX_MENTION_NAME ? kw.substring(0, MAX_MENTION_NAME) : kw;
    }

    /** 是否可作为联想候选：存在、启用中、且不是自己 */
    private boolean isSuggestable(UserAuth user, UUID viewerId) {
        if (user == null || user.getId() == null) {
            return false;
        }
        if (viewerId != null && viewerId.equals(user.getId())) {
            return false;
        }
        return user.getStatus() == null || "active".equals(user.getStatus());
    }

    /**
     * 联想候选的头像：Data URL（内联 base64）不返回。
     * 一次联想最多 10 条，内联头像会让响应膨胀数十倍（每张可达数百 KB），
     * 前端在无头像时用昵称首字色块兜底，视觉信息足够。
     */
    private String compactAvatar(UserAuth user) {
        String avatar = user.getAvatar();
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        return avatar.startsWith("data:") ? null : avatar;
    }

    /** 我关注的人（按关注时间倒序，最多 limit 个） */
    private List<UUID> followingIds(UUID userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        return entityQuery.queryable(DiscussionFollow.class)
                .where(f -> f.userId().eq(userId))
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(1, limit)
                .getData()
                .stream()
                .map(DiscussionFollow::getFollowUserId)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 可被提及的用户：启用中 + 排除自己 +（有关键字时）昵称模糊匹配，信任等级高者优先 */
    private List<UserAuth> searchableUsers(UUID viewerId, String keyword, int limit) {
        return entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.status().eq("active");
                    if (viewerId != null) {
                        u.id().ne(viewerId);
                    }
                    if (!keyword.isEmpty()) {
                        u.nickname().like(keyword);
                    }
                })
                .orderBy(u -> {
                    u.trustLevel().desc();
                    u.createdAt().desc();
                })
                .toPageResult(1, limit)
                .getData();
    }

    private Map<UUID, UserAuth> loadUsers(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, UserAuth> map = new HashMap<>();
        if (distinct.isEmpty()) {
            return map;
        }
        entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().in(distinct))
                .toList()
                .forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private String nicknameOf(UUID userId) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        return nickname(user);
    }

    private String nickname(UserAuth user) {
        if (user == null) {
            return "已注销用户";
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? "民族之友" : user.getNickname();
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        int index = 0;
        while (true) {
            int start = json.indexOf('"', index);
            if (start < 0) {
                break;
            }
            int end = json.indexOf('"', start + 1);
            if (end < 0) {
                break;
            }
            String value = json.substring(start + 1, end).trim();
            if (!value.isEmpty()) {
                list.add(value);
            }
            index = end + 1;
        }
        return list;
    }

    private String excerpt(String text, int max) {
        if (text == null) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > max ? flat.substring(0, max) + "…" : flat;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
