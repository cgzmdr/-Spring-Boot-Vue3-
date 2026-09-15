package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ImageCredit;
import com.czdr.work.service.ImageCreditAdminService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 后台图片署名维护实现（方向 C-4）
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class ImageCreditAdminServiceImpl implements ImageCreditAdminService {

    private static final Set<String> VALID_STATUS = Set.of("verified", "unverified", "original");

    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<ImageCredit> list(String keyword, String status, Pageable pageable) {
        EasyPageResult<ImageCredit> page = entityQuery.queryable(ImageCredit.class)
                .where(c -> {
                    if (keyword != null && !keyword.isBlank()) {
                        c.or(() -> {
                            c.imagePath().like(keyword);
                            c.caption().like(keyword);
                            c.author().like(keyword);
                        });
                    }
                    if (status != null && !status.isBlank()) {
                        c.creditStatus().eq(status);
                    }
                })
                .orderBy(c -> c.imagePath().asc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), page.getData());
    }

    @Override
    public ImageCredit get(String id) {
        ImageCredit c = entityQuery.queryable(ImageCredit.class)
                .where(x -> x.id().eq(parseUuid(id)))
                .firstOrNull();
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return c;
    }

    @Override
    @Transactional
    public void update(String id, ImageCredit body) {
        ImageCredit exist = get(id);

        String status = body.getCreditStatus();
        if (status == null || !VALID_STATUS.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "核实状态取值不合法（可选 verified / unverified / original）");
        }

        boolean hasAuthor = body.getAuthor() != null && !body.getAuthor().isBlank();
        boolean hasLicense = body.getLicense() != null && !body.getLicense().isBlank();

        // 标为「已核实」却没有任何作者/许可信息，等于把待核状态伪装成已完成，
        // 页面仍显示不出署名 —— 因此这里直接拒绝，避免出现「假核实」。
        if ("verified".equals(status) && !hasAuthor && !hasLicense) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "标记为「已核实」时必须至少填写作者或许可，否则页面无法显示署名");
        }

        exist.setCreditStatus(status);
        exist.setAuthor(body.getAuthor());
        exist.setLicense(body.getLicense());
        exist.setLicenseUrl(body.getLicenseUrl());
        exist.setSourceUrl(body.getSourceUrl());
        exist.setSourceSite(body.getSourceSite());
        exist.setCaption(body.getCaption());
        exist.setRemark(body.getRemark());
        if (body.getAttributionRequired() != null) {
            exist.setAttributionRequired(body.getAttributionRequired());
        }
        exist.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(exist).executeRows();
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 格式不正确");
        }
    }
}
