package com.czdr.work.service;

import com.czdr.work.model.resource.FavoriteQueryInfoResource;
import com.czdr.work.model.resource.LikeResource;
import com.czdr.work.model.resource.ShareResource;
import com.czdr.work.model.resource.StatsResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * 内容互动（点赞/收藏/统计/分享）
 *
 * @author cz
 */
public interface InteractionService {
    LikeResource like(String userId, String type, String id);

    LikeResource unlike(String userId, String type, String id);

    void favorite(String userId, String type, String id);

    void unfavorite(String userId, String type, String id);

    StatsResource stats(String type, String id);

    /**
     * 记录一次浏览并返回最新浏览量（公开接口，无需登录）
     */
    long view(String type, String id);

    EasyPageResult<FavoriteQueryInfoResource> favorites(String userId, String type, Pageable pageable);

    ShareResource share(String type, String id);
}
