package com.czdr.work.service;

import com.czdr.work.model.entity.InterestTag;
import com.czdr.work.model.entity.UserInterest;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 兴趣标签服务（方向 D）：标签字典查询 + 用户兴趣维护。
 *
 * <p>用户兴趣是推荐系统的**显式信号**。它存在的意义是解决冷启动：
 * 新注册用户没有任何浏览行为，若只靠行为推荐，结果等同于「热门榜」；
 * 让用户直接勾选感兴趣的题材，立即就能得到有意义的推荐。</p>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

    private final EasyEntityQuery entityQuery;

    /** 按维度分组返回全部启用的标签（前台选择器用） */
    public Map<String, List<InterestTag>> grouped() {
        List<InterestTag> all = entityQuery.queryable(InterestTag.class)
                .where(t -> t.enabled().eq(true))
                .orderBy(t -> {
                    t.dimension().asc();
                    t.orderNum().asc();
                })
                .toList();
        Map<String, List<InterestTag>> out = new LinkedHashMap<>();
        for (InterestTag t : all) {
            out.computeIfAbsent(t.getDimension(), k -> new ArrayList<>()).add(t);
        }
        return out;
    }

    /** 某用户已选标签 ID 列表 */
    public List<String> userTagIds(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return entityQuery.queryable(UserInterest.class)
                .where(ui -> ui.userId().eq(userId))
                .toList()
                .stream()
                .map(ui -> ui.getTagId().toString())
                .toList();
    }

    /**
     * 覆盖式设置用户兴趣标签。
     *
     * <p>采用「全量覆盖」而非增量增删：前端是多选保存，覆盖语义最直观，
     * 也避免反复勾选产生重复行。</p>
     *
     * @param tagIds 标签 ID 列表；为空表示清空兴趣
     * @return 实际保存的标签数
     */
    @Transactional
    public int setUserInterests(UUID userId, List<String> tagIds) {
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        entityQuery.deletable(UserInterest.class)
                .where(ui -> ui.userId().eq(userId))
                .allowDeleteStatement(true)
                .executeRows();

        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }
        // 只接受真实存在的启用标签，避免脏 ID 入库
        List<UUID> ids = new ArrayList<>();
        for (String s : tagIds) {
            try {
                ids.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // 非法 ID 直接跳过
            }
        }
        if (ids.isEmpty()) {
            return 0;
        }
        List<UUID> valid = entityQuery.queryable(InterestTag.class)
                .where(t -> {
                    t.id().in(ids);
                    t.enabled().eq(true);
                })
                .toList()
                .stream().map(InterestTag::getId).toList();

        List<UserInterest> rows = new ArrayList<>(valid.size());
        for (UUID tagId : valid) {
            UserInterest ui = new UserInterest();
            ui.setId(UUID.randomUUID());
            ui.setUserId(userId);
            ui.setTagId(tagId);
            ui.setWeight(1);
            ui.setCreatedAt(LocalDateTime.now());
            rows.add(ui);
        }
        if (!rows.isEmpty()) {
            entityQuery.insertable(rows).executeRows();
        }
        return rows.size();
    }

    /** 用户兴趣摘要（含标签明细，供个人中心展示） */
    public List<InterestTag> userTags(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        List<UserInterest> mine = entityQuery.queryable(UserInterest.class)
                .where(ui -> ui.userId().eq(userId))
                .toList();
        if (mine.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = mine.stream().map(UserInterest::getTagId).toList();
        return entityQuery.queryable(InterestTag.class)
                .where(t -> t.id().in(ids))
                .orderBy(t -> {
                    t.dimension().asc();
                    t.orderNum().asc();
                })
                .toList();
    }
}
