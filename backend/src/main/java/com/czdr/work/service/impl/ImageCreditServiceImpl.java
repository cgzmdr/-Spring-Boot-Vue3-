package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ImageCredit;
import com.czdr.work.model.resource.ImageCreditResource;
import com.czdr.work.model.resource.ImageCreditStatsResource;
import com.czdr.work.service.ImageCreditService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 图片署名实现（方向 C-4）
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class ImageCreditServiceImpl implements ImageCreditService {

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "verified", "已核实",
            "unverified", "来源待核",
            "original", "原创 / 无需署名"
    );

    private final EasyEntityQuery entityQuery;

    @Override
    public List<ImageCreditResource> findByContent(String targetType, String targetId) {
        if (targetType == null || targetType.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容类型不能为空");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(targetId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容 ID 格式不正确");
        }
        List<ImageCredit> list = entityQuery.queryable(ImageCredit.class)
                .where(c -> {
                    c.targetType().eq(targetType);
                    c.targetId().eq(uuid);
                })
                .toList();
        return list.stream().map(this::toResource).toList();
    }

    @Override
    public List<ImageCreditResource> findByPaths(String paths) {
        if (paths == null || paths.isBlank()) {
            return List.of();
        }
        List<String> wanted = Arrays.stream(paths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<ImageCredit> list = entityQuery.queryable(ImageCredit.class)
                .where(c -> c.imagePath().in(wanted))
                .toList();
        // 按请求顺序返回，便于调用方按下标对应
        Map<String, ImageCredit> byPath = new LinkedHashMap<>();
        for (ImageCredit c : list) {
            byPath.put(c.getImagePath(), c);
        }
        List<ImageCreditResource> out = new ArrayList<>();
        for (String p : wanted) {
            ImageCredit c = byPath.get(p);
            if (c != null) {
                out.add(toResource(c));
            }
        }
        return out;
    }

    @Override
    public ImageCreditStatsResource stats() {
        List<ImageCredit> all = entityQuery.queryable(ImageCredit.class).toList();
        long total = all.size();
        long verified = all.stream().filter(c -> "verified".equals(c.getCreditStatus())).count();
        long unverified = all.stream().filter(c -> "unverified".equals(c.getCreditStatus())).count();
        long original = all.stream().filter(c -> "original".equals(c.getCreditStatus())).count();
        // 合规缺口：需要署名但尚未核实
        long pending = all.stream()
                .filter(c -> !"verified".equals(c.getCreditStatus()))
                .filter(c -> c.getAttributionRequired() == null || c.getAttributionRequired())
                .count();
        double rate = total == 0 ? 0 : Math.round(verified * 1000.0 / total) / 10.0;
        return new ImageCreditStatsResource(total, verified, unverified, original, pending, rate);
    }

    private ImageCreditResource toResource(ImageCredit c) {
        return new ImageCreditResource(
                c.getImagePath(),
                c.getCaption(),
                c.getCreditStatus(),
                STATUS_LABELS.getOrDefault(c.getCreditStatus(), c.getCreditStatus()),
                c.getAuthor(),
                c.getLicense(),
                c.getLicenseUrl(),
                c.getSourceUrl(),
                c.getSourceSite(),
                c.getAttributionRequired(),
                c.getRemark(),
                buildCreditLine(c)
        );
    }

    /**
     * 生成可直接展示的署名文本。
     * <p>仅当**作者与许可都已核实**时才返回内容 —— 未核实时返回 null，
     * 由前端显示「来源待核」。绝不拼出「作者：未知」这类看起来像署名的假文本。</p>
     */
    private String buildCreditLine(ImageCredit c) {
        if (!"verified".equals(c.getCreditStatus())) {
            return null;
        }
        String author = c.getAuthor() == null ? "" : c.getAuthor().trim();
        String license = c.getLicense() == null ? "" : c.getLicense().trim();
        if (author.isEmpty() && license.isEmpty()) {
            return null;
        }
        if (author.isEmpty()) {
            return license;
        }
        if (license.isEmpty()) {
            return author;
        }
        return author + " / " + license;
    }
}
