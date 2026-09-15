package com.czdr.work.service;

import com.czdr.work.model.entity.InterestTag;
import com.czdr.work.model.entity.SearchDocument;
import com.czdr.work.model.entity.UserBehavior;
import com.czdr.work.model.entity.UserInterest;
import com.czdr.work.model.resource.RecommendationResource;
import com.czdr.work.model.resource.RecommendationResource.RecoItem;
import com.czdr.work.util.JsonUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 推荐引擎（方向 D）。
 *
 * <p><b>混合策略（四路召回 + 加权融合）</b>，按可用信号强弱自动降级：</p>
 * <ol>
 *   <li><b>兴趣标签召回</b>（显式信号）：用户勾选的民族/地域/类型/主题 → 直接召回对应内容。
 *       这是冷启动的主要解法——新用户没有任何行为也能得到像样的推荐。</li>
 *   <li><b>行为协同召回</b>（隐式信号）：从浏览/点赞/收藏历史中提取「兴趣民族 / 兴趣分类」，
 *       召回同类内容；并排除已看过的条目。</li>
 *   <li><b>内容相似召回</b>：以最近一次浏览的内容为种子，召回同民族或同分类的内容
 *       （item-based 相似度，用民族/分类作为特征）。</li>
 *   <li><b>热度兜底</b>：以上都无信号时按热度返回（热门内容）。</li>
 * </ol>
 *
 * <p><b>为什么不做经典协同过滤（User-CF）：</b>实测全站 5 个用户、行为共 32 条，
 * 用户-物品矩阵极度稀疏，User-CF 的相似度不可用（任意两用户共同物品几乎为 0）。
 * 因此这里的「协同」是**基于内容特征的协同**，并在接口中如实标注可信度。</p>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    /** 行为权重：浏览 1 / 搜索 2 / 点赞 3 / 收藏 5 */
    public static final Map<String, Integer> ACTION_WEIGHT = Map.of(
            "view", 1, "search", 2, "like", 3, "favorite", 5);

    /** 画像可信度达到该行为量才算「真正个性化」 */
    private static final int CONFIDENT_BEHAVIORS = 30;

    private final EasyEntityQuery entityQuery;

    /**
     * 为用户生成推荐。
     *
     * @param userId 用户 ID；为 null（游客）时退化为「兴趣标签/热度」推荐
     * @param size   返回条数
     * @param excludeType 需要排除的内容类型（如详情页推荐时排除自身类型），可为 null
     */
    public RecommendationResource recommend(UUID userId, int size, String excludeType) {
        List<String> reasons = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // ---- 信号 1：显式兴趣标签 ----
        Set<String> interestEthnics = new LinkedHashSet<>();
        Set<String> interestRegions = new LinkedHashSet<>();
        Set<String> interestTypes = new LinkedHashSet<>();
        Set<String> interestTopics = new LinkedHashSet<>();
        int interestCount = 0;
        if (userId != null) {
            List<UserInterest> interests = entityQuery.queryable(UserInterest.class)
                    .where(ui -> ui.userId().eq(userId))
                    .toList();
            interestCount = interests.size();
            if (!interests.isEmpty()) {
                List<UUID> tagIds = interests.stream().map(UserInterest::getTagId).toList();
                List<InterestTag> tags = entityQuery.queryable(InterestTag.class)
                        .where(t -> {
                            t.id().in(tagIds);
                            t.enabled().eq(true);
                        })
                        .toList();
                for (InterestTag t : tags) {
                    switch (t.getDimension()) {
                        case "ethnic" -> interestEthnics.add(t.getName());
                        case "region" -> interestRegions.add(t.getName());
                        case "type" -> interestTypes.add(t.getName());
                        case "topic" -> interestTopics.add(t.getName());
                        default -> { }
                    }
                }
            }
        }

        // ---- 信号 2：隐式行为 ----
        Set<String> behaviorEthnics = new LinkedHashSet<>();
        Set<String> behaviorCategories = new LinkedHashSet<>();
        Set<String> viewedDocKeys = new LinkedHashSet<>();
        int behaviorCount = 0;
        if (userId != null) {
            List<UserBehavior> behaviors = entityQuery.queryable(UserBehavior.class)
                    .where(b -> b.userId().eq(userId))
                    .orderBy(b -> b.createdAt().desc())
                    .limit(200)
                    .toList();
            behaviorCount = behaviors.size();

            // 只看有目标内容的行为（搜索行为只用于关键词，不直接提供民族/分类）
            List<UUID> targetIds = behaviors.stream()
                    .map(UserBehavior::getTargetId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            Map<String, SearchDocument> docByKey = new LinkedHashMap<>();
            if (!targetIds.isEmpty()) {
                for (SearchDocument d : entityQuery.queryable(SearchDocument.class)
                        .where(sd -> sd.docId().in(targetIds))
                        .toList()) {
                    docByKey.put(d.getDocType() + ":" + d.getDocId(), d);
                }
            }
            for (UserBehavior b : behaviors) {
                if (b.getTargetId() == null || b.getTargetType() == null) {
                    continue;
                }
                String key = b.getTargetType() + ":" + b.getTargetId();
                SearchDocument d = docByKey.get(key);
                if (d == null) {
                    continue;
                }
                // 收藏/点赞记入「已看过」；纯浏览也计入（避免重复推荐已读）
                viewedDocKeys.add(key);
                if (d.getEthnicName() != null) {
                    behaviorEthnics.add(d.getEthnicName());
                }
                if (d.getCategory() != null) {
                    behaviorCategories.add(d.getCategory());
                }
            }
        }

        boolean hasInterest = !interestEthnics.isEmpty() || !interestRegions.isEmpty()
                || !interestTypes.isEmpty() || !interestTopics.isEmpty();
        boolean hasBehavior = !behaviorEthnics.isEmpty() || !behaviorCategories.isEmpty();

        // 画像可信度：行为样本越少越不可信（诚实性）
        double confidence = userId == null ? 0d
                : Math.min(1d, behaviorCount / (double) CONFIDENT_BEHAVIORS);

        // ---- 候选召回 ----
        Map<String, ScoredDoc> scored = new LinkedHashMap<>();
        List<SearchDocument> pool = entityQuery.queryable(SearchDocument.class).toList();

        for (SearchDocument d : pool) {
            String key = d.getDocType() + ":" + d.getDocId();
            if (viewedDocKeys.contains(key)) {
                continue; // 不重复推荐已读
            }
            if (excludeType != null && excludeType.equals(d.getDocType())) {
                continue;
            }
            double score = 0;
            String reason = null;

            // 1) 兴趣标签（显式）—— 权重最高
            if (d.getEthnicName() != null && interestEthnics.contains(d.getEthnicName())) {
                score += 50;
                reason = "你关注了" + d.getEthnicName();
            }
            if (d.getRegion() != null && interestRegions.contains(d.getRegion())) {
                score += 25;
                reason = reason == null ? "你关注了" + d.getRegion() : reason;
            }
            if (d.getCategory() != null && interestTopics.contains(d.getCategory())) {
                score += 20;
                reason = reason == null ? "你关注了「" + d.getCategory() + "」" : reason;
            }
            if (typeLabelToDocType(interestTypes).contains(d.getDocType())) {
                score += 10;
                reason = reason == null ? "你关注这类内容" : reason;
            }

            // 2) 行为协同（隐式）—— 比显式兴趣低，但仍是强信号
            if (d.getEthnicName() != null && behaviorEthnics.contains(d.getEthnicName())) {
                score += 30;
                reason = reason == null ? "与你浏览过的内容相关" : reason;
            }
            if (d.getCategory() != null && behaviorCategories.contains(d.getCategory())) {
                score += 12;
                reason = reason == null ? "与你浏览过的内容同类" : reason;
            }

            // 3) 热度（兜底，最多 +10）
            int pop = d.getPopularity() == null ? 0 : d.getPopularity();
            score += Math.min(pop, 100) * 0.1;

            // 4) 时效轻微加权（最多 +5，一年内线性衰减）
            if (d.getContentAt() != null) {
                long days = Math.max(0, Duration.between(d.getContentAt(), LocalDateTime.now()).toDays());
                score += Math.max(0, 5 - days / 73.0);
            }

            // 民族类内容略微优先（站点的核心维度）
            if ("ethnic".equals(d.getDocType())) {
                score += 3;
            }

            if (reason == null && score > 0) {
                reason = "热门内容";
            }
            if (score > 0) {
                scored.put(key, new ScoredDoc(d, score, reason));
            }
        }

        // ---- 排序与截断 ----
        List<ScoredDoc> ordered = scored.values().stream()
                .sorted(Comparator.comparingDouble((ScoredDoc s) -> s.score).reversed()
                        .thenComparing(s -> s.doc.getTitle()))
                .limit(Math.max(size, 1))
                .toList();

        // ---- 如实描述依据 ----
        String basis;
        String basisLabel;
        if (hasInterest && hasBehavior) {
            basis = "personalized";
            basisLabel = "基于你的兴趣标签与浏览历史";
        } else if (hasInterest) {
            basis = "interest";
            basisLabel = "基于你选择的兴趣标签";
        } else if (hasBehavior) {
            basis = "personalized";
            basisLabel = "基于你的浏览历史";
        } else if (userId != null) {
            basis = "popularity";
            basisLabel = "按热度推荐（你还没有选择兴趣或产生浏览记录）";
        } else {
            basis = "popularity";
            basisLabel = "热门内容（登录并选择兴趣后可获得个性化推荐）";
        }

        // 数据说明：如实告知样本量与可信度
        String dataNote;
        if (behaviorCount == 0 && interestCount == 0) {
            dataNote = "当前没有可用的个人行为数据，本次推荐来自内容热度。";
        } else if (confidence < 0.5) {
            dataNote = String.format(
                    "当前仅有 %d 条行为记录、%d 个兴趣标签，样本较少，推荐以内容相似度与热度为主。",
                    behaviorCount, interestCount);
        } else {
            dataNote = String.format("基于 %d 条行为记录与 %d 个兴趣标签计算。",
                    behaviorCount, interestCount);
        }

        List<RecoItem> list = ordered.stream().map(s -> new RecoItem(
                s.doc.getDocType(), String.valueOf(s.doc.getDocId()), s.doc.getUrl(),
                s.doc.getTitle(), s.doc.getSummary(), s.doc.getEthnicName(), s.doc.getCategory(),
                s.doc.getCoverImage(), s.doc.getThemeColor(),
                Math.round(s.score * 10) / 10d, s.reason)).toList();

        return new RecommendationResource(list, basis, basisLabel, dataNote,
                Math.round(confidence * 100) / 100d, behaviorCount, interestCount);
    }

    /** 内部打分载体 */
    private record ScoredDoc(SearchDocument doc, double score, String reason) {
    }

    /** 兴趣标签中「内容类型」维度的中文名 → doc_type */
    private Set<String> typeLabelToDocType(Set<String> labels) {
        Map<String, String> map = Map.of(
                "民族", "ethnic", "节日", "festival", "艺术", "art", "美食", "food",
                "风俗", "custom", "人物", "person", "自治地方", "area", "传统体育", "sport");
        Set<String> out = new LinkedHashSet<>();
        for (String l : labels) {
            String t = map.get(l);
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 记录用户行为（隐式信号采集）。
     *
     * <p>未登录（userId 为 null）不记录：无法归属到个人的行为不构成个人画像，
     * 记录它只会制造「有数据」的假象。</p>
     */
    public void record(UUID userId, String action, String targetType, UUID targetId, String keyword) {
        if (userId == null) {
            return;
        }
        try {
            UserBehavior b = new UserBehavior();
            b.setId(UUID.randomUUID());
            b.setUserId(userId);
            b.setAction(action);
            b.setTargetType(targetType);
            b.setTargetId(targetId);
            b.setKeyword(keyword);
            b.setWeight(ACTION_WEIGHT.getOrDefault(action, 1));
            b.setCreatedAt(LocalDateTime.now());
            entityQuery.insertable(b).executeRows();
        } catch (Exception e) {
            // 行为采集失败不能影响主流程
            log.warn("行为记录失败（忽略）：{}", e.getMessage());
        }
    }
}
