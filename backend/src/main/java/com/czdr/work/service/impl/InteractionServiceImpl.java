package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.*;
import com.czdr.work.model.entity.proxy.FavoriteProxy;
import com.czdr.work.model.entity.proxy.LikeRecordProxy;
import com.czdr.work.model.resource.FavoriteQueryInfoResource;
import com.czdr.work.model.resource.LikeResource;
import com.czdr.work.model.resource.ShareResource;
import com.czdr.work.model.resource.StatsResource;
import com.czdr.work.service.InteractionService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {
    private static final Set<String> TYPES = Set.of("ethnic", "festival", "art", "topic");

    private final EasyEntityQuery entityQuery;

    @Override
    public LikeResource like(String userId, String type, String id) {
        UUID entryId = validate(type, id);
        if (queryLike(userId, type, entryId) != null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "已点赞，请勿重复操作");
        }
        LikeRecord record = new LikeRecord();
        record.setId(UUID.randomUUID());
        record.setUserId(UUID.fromString(userId));
        record.setEntryType(type);
        record.setEntryId(entryId);
        record.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(record).executeRows();
        changeLikeCount(type, entryId, 1);
        return new LikeResource(true, getLikeCount(type, entryId));
    }

    @Override
    public LikeResource unlike(String userId, String type, String id) {
        UUID entryId = validate(type, id);
        LikeRecord record = queryLike(userId, type, entryId);
        if (record == null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "尚未点赞");
        }
        entityQuery.deletable(record).allowDeleteStatement(true).executeRows();
        changeLikeCount(type, entryId, -1);
        return new LikeResource(false, getLikeCount(type, entryId));
    }

    @Override
    public void favorite(String userId, String type, String id) {
        UUID entryId = validate(type, id);
        Favorite favorite = queryFavorite(userId, type, entryId);
        if (favorite != null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "已收藏，请勿重复操作");
        }
        Favorite record = new Favorite();
        record.setId(UUID.randomUUID());
        record.setUserId(UUID.fromString(userId));
        record.setEntryType(type);
        record.setEntryId(entryId);
        record.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(record).executeRows();
    }

    @Override
    public void unfavorite(String userId, String type, String id) {
        UUID entryId = validate(type, id);
        Favorite favorite = queryFavorite(userId, type, entryId);
        if (favorite == null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "尚未收藏");
        }
        entityQuery.deletable(favorite).allowDeleteStatement(true).executeRows();
    }

    @Override
    public StatsResource stats(String type, String id) {
        UUID entryId = validate(type, id);
        long likeCount = getLikeCount(type, entryId);
        long favoriteCount = entityQuery.queryable(Favorite.class)
                .where(f -> {
                    f.entryType().eq(type);
                    f.entryId().eq(entryId);
                })
                .count();
        return new StatsResource(likeCount, favoriteCount, getViewCount(type, entryId));
    }

    @Override
    public long view(String type, String id) {
        UUID entryId = validate(type, id);
        changeViewCount(type, entryId, 1);
        return getViewCount(type, entryId);
    }

    @Override
    public EasyPageResult<FavoriteQueryInfoResource> favorites(String userId, String type, Pageable pageable) {
        UUID uid = UUID.fromString(userId);
        EasyPageResult<Favorite> pageResult = entityQuery.queryable(Favorite.class)
                .where(f -> {
                    f.userId().eq(uid);
                    if (type != null && !type.isBlank()) {
                        f.entryType().eq(type);
                    }
                })
                .orderBy(f -> f.createdAt().desc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<FavoriteQueryInfoResource> data = pageResult.getData().stream()
                .map(f -> {
                    String[] resolved = resolveEntry(f.getEntryType(), f.getEntryId());
                    return new FavoriteQueryInfoResource(
                            f.getId().toString(),
                            f.getEntryType(),
                            f.getEntryId().toString(),
                            resolved[0],
                            resolved[1],
                            f.getCreatedAt()
                    );
                })
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public ShareResource share(String type, String id) {
        validate(type, id);
        return new ShareResource("/share?type=" + type + "&id=" + id, null);
    }

    /**
     * 校验内容类型与内容存在性，返回解析后的 UUID
     */
    private UUID validate(String type, String id) {
        if (type == null || !TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的内容类型: " + type);
        }
        UUID entryId;
        try {
            entryId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容 id 格式错误");
        }
        if (!entryExists(type, entryId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entryId;
    }

    private boolean entryExists(String type, UUID entryId) {
        return switch (type) {
            case "ethnic" -> entityQuery.queryable(EthnicGroup.class)
                    .where(e -> e.id().eq(entryId)).firstOrNull() != null;
            case "festival" -> entityQuery.queryable(Festival.class)
                    .where(e -> e.id().eq(entryId)).firstOrNull() != null;
            case "art" -> entityQuery.queryable(Art.class)
                    .where(e -> e.id().eq(entryId)).firstOrNull() != null;
            case "topic" -> entityQuery.queryable(Topic.class)
                    .where(e -> e.id().eq(entryId)).firstOrNull() != null;
            default -> false;
        };
    }

    /**
     * 解析内容名称与封面（多态）
     */
    private String[] resolveEntry(String type, UUID entryId) {
        return switch (type) {
            case "ethnic" -> {
                EthnicGroup e = entityQuery.queryable(EthnicGroup.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                yield e == null ? new String[]{null, null} : new String[]{e.getName(), e.getCoverImage()};
            }
            case "festival" -> {
                Festival e = entityQuery.queryable(Festival.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                yield e == null ? new String[]{null, null} : new String[]{e.getName(), e.getCoverImage()};
            }
            case "art" -> {
                Art e = entityQuery.queryable(Art.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                yield e == null ? new String[]{null, null} : new String[]{e.getName(), e.getCoverImage()};
            }
            case "topic" -> {
                Topic e = entityQuery.queryable(Topic.class)
                        .where(x -> x.id().eq(entryId)).firstOrNull();
                yield e == null ? new String[]{null, null} : new String[]{e.getTitle(), e.getCoverImage()};
            }
            default -> new String[]{null, null};
        };
    }

    private LikeRecord queryLike(String userId, String type, UUID entryId) {
        return entityQuery.queryable(LikeRecord.class)
                .where(l -> {
                    l.userId().eq(UUID.fromString(userId));
                    l.entryType().eq(type);
                    l.entryId().eq(entryId);
                })
                .firstOrNull();
    }

    private Favorite queryFavorite(String userId, String type, UUID entryId) {
        return entityQuery.queryable(Favorite.class)
                .where(f -> {
                    f.userId().eq(UUID.fromString(userId));
                    f.entryType().eq(type);
                    f.entryId().eq(entryId);
                })
                .firstOrNull();
    }

    /**
     * 点赞计数（Redis 为热读主存储，此表持久化）
     */
    private long getLikeCount(String type, UUID entryId) {
        LikeCounter counter = entityQuery.queryable(LikeCounter.class)
                .where(c -> {
                    c.entryType().eq(type);
                    c.entryId().eq(entryId);
                })
                .firstOrNull();
        return counter == null ? 0 : counter.getCount();
    }

    private void changeLikeCount(String type, UUID entryId, long delta) {
        LikeCounter counter = entityQuery.queryable(LikeCounter.class)
                .where(c -> {
                    c.entryType().eq(type);
                    c.entryId().eq(entryId);
                })
                .firstOrNull();
        if (counter == null) {
            entityQuery.insertable(new LikeCounter(type, entryId, 1L, LocalDateTime.now())).executeRows();
            return;
        }
        counter.setCount(Math.max(0, counter.getCount() + delta));
        counter.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(counter).executeRows();
    }

    /**
     * 浏览量计数（与点赞计数同构，独立表持久化）
     */
    private long getViewCount(String type, UUID entryId) {
        ViewCounter counter = entityQuery.queryable(ViewCounter.class)
                .where(c -> {
                    c.entryType().eq(type);
                    c.entryId().eq(entryId);
                })
                .firstOrNull();
        return counter == null ? 0 : counter.getCount();
    }

    private void changeViewCount(String type, UUID entryId, long delta) {
        ViewCounter counter = entityQuery.queryable(ViewCounter.class)
                .where(c -> {
                    c.entryType().eq(type);
                    c.entryId().eq(entryId);
                })
                .firstOrNull();
        if (counter == null) {
            entityQuery.insertable(new ViewCounter(type, entryId, 1L, LocalDateTime.now())).executeRows();
            return;
        }
        counter.setCount(counter.getCount() + delta);
        counter.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(counter).executeRows();
    }
}
