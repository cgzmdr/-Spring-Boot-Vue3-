package com.czdr.work.service.impl;

import com.czdr.work.comment.convert.ArtConvert;
import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.comment.convert.FestivalConvert;
import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
import com.czdr.work.model.entity.proxy.FestivalProxy;
import com.czdr.work.model.resource.SearchGroupResource;
import com.czdr.work.model.resource.SearchResultResource;
import com.czdr.work.service.RedisService;
import com.czdr.work.service.SearchService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 全局搜索：按关键词模糊匹配民族/节日/艺术，支持拼音
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final String HOT_KEY = "search:hot";
    private static final int HOT_SIZE = 10;

    private final EasyEntityQuery entityQuery;
    private final RedisService redisService;

    @Override
    public SearchResultResource search(String q, String type, int page, int size) {
        String keyword = q == null ? "" : q.trim();
        String queryType = type == null || type.isBlank() ? "all" : type;
        SearchGroupResource ethnic = null;
        SearchGroupResource festival = null;
        SearchGroupResource art = null;
        if ("all".equals(queryType) || "ethnic".equals(queryType)) {
            ethnic = searchEthnic(keyword, page, size);
        }
        if ("all".equals(queryType) || "festival".equals(queryType)) {
            festival = searchFestival(keyword, page, size);
        }
        if ("all".equals(queryType) || "art".equals(queryType)) {
            art = searchArt(keyword, page, size);
        }
        if (!keyword.isEmpty()) {
            recordHot(keyword);
        }
        return new SearchResultResource(ethnic, festival, art);
    }

    @Override
    public List<String> hot() {
        Object value = redisService.getString(HOT_KEY);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Arrays.asList(com.czdr.work.util.JsonUtil.toStringArray(value.toString()));
    }

    private SearchGroupResource searchEthnic(String keyword, int page, int size) {
        EasyPageResult<EthnicGroup> result = entityQuery.queryable(EthnicGroup.class)
                .where(e -> {
                    e.status().eq("published");
                    if (!keyword.isEmpty()) {
                        e.or(() -> {
                            e.name().like(keyword);
                            e.pinyin().like(keyword);
                            e.nameEn().like(keyword);
                        });
                    }
                })
                .orderBy(EthnicGroupProxy::orderNum)
                .toPageResult(page, size);
        return new SearchGroupResource(result.getTotal(),
                result.getData().stream().map(EthnicConvert::toInfoModel).toList());
    }

    private SearchGroupResource searchFestival(String keyword, int page, int size) {
        EasyPageResult<Festival> result = entityQuery.queryable(Festival.class)
                .where(f -> {
                    f.status().eq("published");
                    if (!keyword.isEmpty()) {
                        f.or(() -> {
                            f.name().like(keyword);
                            f.nameEn().like(keyword);
                        });
                    }
                })
                .orderBy(FestivalProxy::orderNum)
                .toPageResult(page, size);
        return new SearchGroupResource(result.getTotal(),
                result.getData().stream().map(FestivalConvert::toInfoModel).toList());
    }

    private SearchGroupResource searchArt(String keyword, int page, int size) {
        EasyPageResult<Art> result = entityQuery.queryable(Art.class)
                .where(a -> {
                    a.status().eq("published");
                    if (!keyword.isEmpty()) {
                        a.or(() -> {
                            a.name().like(keyword);
                            a.nameEn().like(keyword);
                        });
                    }
                })
                .orderBy(ArtProxy::orderNum)
                .toPageResult(page, size);
        return new SearchGroupResource(result.getTotal(),
                result.getData().stream().map(ArtConvert::toInfoModel).toList());
    }

    /**
     * 记录搜索词到热门榜（Redis，去重置顶，保留前 N）
     */
    private void recordHot(String keyword) {
        List<String> list = new ArrayList<>(hot());
        list.remove(keyword);
        list.add(0, keyword);
        if (list.size() > HOT_SIZE) {
            list = list.subList(0, HOT_SIZE);
        }
        redisService.setString(HOT_KEY, list, 30, TimeUnit.DAYS);
    }
}
