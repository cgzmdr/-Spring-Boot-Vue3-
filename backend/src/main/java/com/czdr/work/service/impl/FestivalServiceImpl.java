package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.FestivalQueryBind;
import com.czdr.work.comment.convert.FestivalConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.proxy.FestivalProxy;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import com.czdr.work.model.resource.FestivalCalendarResource;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.czdr.work.service.FestivalService;
import com.czdr.work.service.calendar.LunarFestivalDateResolver;
import com.czdr.work.util.LunarCalendarUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class FestivalServiceImpl implements FestivalService {
    private final EasyEntityQuery entityQuery;

    @Override
    public EasyPageResult<FestivalQueryInfoResource> find(FestivalQueryInfoRequest request, Pageable pageable) {
        Map<String, Consumer<FestivalProxy>> binds = new FestivalQueryBind(request).customize();
        EasyPageResult<Festival> pageResult = entityQuery.queryable(Festival.class)
                .where(f -> {
                    f.status().eq("published");
                    request.toMap().forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank()) {
                            Consumer<FestivalProxy> consumer = binds.get(key);
                            if (consumer != null) {
                                consumer.accept(f);
                            }
                        }
                    });
                })
                .include(FestivalProxy::ethnicGroup)
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        List<FestivalQueryInfoResource> data = pageResult.getData().stream()
                .map(FestivalConvert::toInfoModel)
                .toList();
        return new DefaultPageResult<>(pageResult.getTotal(), data);
    }

    @Override
    public Festival find(String id) {
        UUID uuid = UUID.fromString(id);
        Festival festival = entityQuery.queryable(Festival.class)
                .where(f -> {
                    f.id().eq(uuid);
                    f.status().eq("published");
                })
                .include(FestivalProxy::ethnicGroup)
                .firstOrNull();
        if (festival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return festival;
    }

    /**
     * 节日日历：全部已发布节日 → 按公历月份聚合。
     * <p>一次性把 192 条节日查出来在内存中换算与分组：数据量小、且农历换算是纯计算，
     * 放在 SQL 里反而无法表达（数据库没有农历函数）。</p>
     */
    @Override
    public FestivalCalendarResource calendar(Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        if (targetYear < LunarCalendarUtil.MIN_YEAR || targetYear > LunarCalendarUtil.MAX_YEAR) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "年份需在 " + LunarCalendarUtil.MIN_YEAR + "–" + LunarCalendarUtil.MAX_YEAR + " 之间");
        }

        List<Festival> all = entityQuery.queryable(Festival.class)
                .where(f -> f.status().eq("published"))
                .include(FestivalProxy::ethnicGroup)
                .toList();

        LocalDate today = LocalDate.now();
        List<FestivalCalendarResource.FestivalItem> items = new ArrayList<>();
        for (Festival f : all) {
            LunarFestivalDateResolver.Resolved resolved =
                    LunarFestivalDateResolver.resolve(f.getSolarDate(), f.getLunarDate(), targetYear);
            if (resolved == null) {
                // 历法无法换算（伊斯兰历 / 傣历等）：不猜测日期，日历中略过
                continue;
            }
            LocalDate date = resolved.date();
            items.add(new FestivalCalendarResource.FestivalItem(
                    f.getId().toString(),
                    f.getName(),
                    f.getNameEn(),
                    f.getEthnicGroup() != null ? f.getEthnicGroup().getName() : null,
                    f.getType(),
                    date.toString(),
                    date.getDayOfMonth(),
                    f.getLunarDate(),
                    resolved.source(),
                    ChronoUnit.DAYS.between(today, date)
            ));
        }

        items.sort(Comparator.comparing(FestivalCalendarResource.FestivalItem::date));

        List<FestivalCalendarResource.Month> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            List<FestivalCalendarResource.FestivalItem> inMonth = items.stream()
                    .filter(i -> LocalDate.parse(i.date()).getMonthValue() == month)
                    .toList();
            months.add(new FestivalCalendarResource.Month(month, inMonth.size(), inMonth));
        }

        // 今日节日（优先取当天，其次取同年同月日；都没有则为空）
        FestivalCalendarResource.FestivalItem todayItem = items.stream()
                .filter(i -> i.daysFromToday() == 0)
                .findFirst()
                .orElse(null);

        // 未来 30 天（含今天）
        List<FestivalCalendarResource.FestivalItem> upcoming = items.stream()
                .filter(i -> i.daysFromToday() >= 0 && i.daysFromToday() <= 30)
                .sorted(Comparator.comparingLong(FestivalCalendarResource.FestivalItem::daysFromToday))
                .toList();

        return new FestivalCalendarResource(targetYear, months, todayItem, upcoming);
    }
}
