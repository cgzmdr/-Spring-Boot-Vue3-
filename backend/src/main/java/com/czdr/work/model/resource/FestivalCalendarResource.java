package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 节日日历（按公历月份聚合）
 *
 * @author cz
 */
@Schema(description = "节日日历")
public record FestivalCalendarResource(
        @Schema(description = "年份") int year,
        @Schema(description = "按月分组的节日（1–12 月）") List<Month> months,
        @Schema(description = "该年今日 / 最近一个节日") FestivalItem today,
        @Schema(description = "未来 30 天内的节日（升序）") List<FestivalItem> upcoming
) {
    @Schema(description = "一个公历月")
    public record Month(
            @Schema(description = "月份（1–12）") int month,
            @Schema(description = "该月节日数量") int count,
            @Schema(description = "该月节日列表（按日升序）") List<FestivalItem> festivals
    ) {
    }

    @Schema(description = "日历中的一个节日")
    public record FestivalItem(
            @Schema(description = "节日 ID") String id,
            @Schema(description = "节日名称") String name,
            @Schema(description = "节日英文名") String nameEn,
            @Schema(description = "所属民族") String ethnicGroupName,
            @Schema(description = "节日类型") String type,
            @Schema(description = "公历日期（yyyy-MM-dd），由农历换算或原始公历得到") String date,
            @Schema(description = "公历日（1–31）") int day,
            @Schema(description = "农历日期原文（可能为空）") String lunarDate,
            @Schema(description = "日期来源：solar 原始公历 / lunar 农历换算 / approx 估算（仅精确到月）") String dateSource,
            @Schema(description = "距今的天数（负数表示已过去）") long daysFromToday
    ) {
    }
}
