package com.czdr.work.service.calendar;

import com.czdr.work.util.LunarCalendarUtil;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把数据库中自由文本形式的节日日期解析为某一年的公历日期。
 *
 * <p>数据实态（出自国家民委等权威源整理，表述高度不统一）：
 * <ul>
 *   <li>公历：{@code 2026-03-21}（仅 33/192 条有值）、{@code 4 月 5 日}</li>
 *   <li>农历精确：{@code 农历正月初一}、{@code 农历八月十五}、{@code 农历十二月初八}</li>
 *   <li>农历区间：{@code 农历正月初一至十五}、{@code 农历七月十二日至九月十五日}</li>
 *   <li>农历模糊：{@code 农历八月}、{@code 农历七八月}、{@code 农历五月（夏至前后）}</li>
 *   <li>其他历法：{@code 伊斯兰历十月一日}、{@code 傣历九月十五日} —— <b>不换算</b>，仅记录月份提示</li>
 * </ul>
 *
 * <p>解析策略：精确到「日」的农历表述才做换算（可信）；只精确到「月」的按该月十五
 * 估算并标记 {@code approx}，前端据此提示；无法解析的返回 {@code null} 并保留原文。
 *
 * @author cz
 */
public final class LunarFestivalDateResolver {

    private LunarFestivalDateResolver() {
    }

    /** 日期来源标记 */
    public static final String SOURCE_SOLAR = "solar";
    public static final String SOURCE_LUNAR = "lunar";
    public static final String SOURCE_APPROX = "approx";

    /** 解析结果 */
    public record Resolved(LocalDate date, String source) {
    }

    /**
     * 农历月份前缀：「农历」「农历闰」+ 月份中文 + 「月」
     * <p>月份允许单字（正/一/二/…/冬/腊）或双字（十一 / 十二）。
     */
    private static final String LUNAR_PREFIX = "^农历(闰)?([正一二三四五六七八九十冬腊]{1,2})月";

    /**
     * 农历日期（精确到日）。兼容数据库里混用的三种写法：
     * <ul>
     *   <li>标准：初一…初十、十一…二十、廿一…廿九、三十</li>
     *   <li>省略「初 / 廿」：九日、二十九、二十八</li>
     *   <li>带或不带末尾「日」</li>
     * </ul>
     */
    private static final Pattern LUNAR_FULL = Pattern.compile(
            LUNAR_PREFIX
                    + "(初[一二三四五六七八九十]|十[一二三四五六七八九]?|二十|廿[一二三四五六七八九]"
                    + "|三十|[一二三四五六七八九])日?$");

    /** 农历「某月」或「某月（说明）」——只精确到月；月份允许「七八月」这种双月写法（取第一个月） */
    private static final Pattern LUNAR_MONTH_ONLY = Pattern.compile(LUNAR_PREFIX);

    /** 公历 ISO */
    private static final Pattern SOLAR_ISO = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");

    /** 公历「3 月 21 日 / 3月21日」 */
    private static final Pattern SOLAR_CN = Pattern.compile("^(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日?$");

    /**
     * 解析某年的节日日期。
     *
     * @param solarDate  数据库 solar_date 字段（可能为 null；实体中为 LocalDate，故为 Object）
     * @param lunarDate  数据库 lunar_date 字段（可能为 null）
     * @param year       目标年份
     * @return 解析结果；无法解析返回 {@code null}
     */
    public static Resolved resolve(Object solarDate, String lunarDate, int year) {
        // 1) 已有公历日期：直接采用（原始数据最可信）
        Resolved fromSolar = parseSolar(solarDate, year);
        if (fromSolar != null) {
            return fromSolar;
        }

        if (lunarDate == null || lunarDate.isBlank()) {
            return null;
        }
        String text = lunarDate.trim();

        // 2) 非农历历法（伊斯兰历 / 傣历等）：无法用农历表换算，避免给出错误日期
        if (!text.startsWith("农历") && !text.startsWith("藏历")) {
            return null;
        }
        // 藏历与农历接近但不完全一致，这里只在农历前缀时做精确换算
        if (text.startsWith("藏历")) {
            return null;
        }

        // 3) 农历精确到日
        Matcher full = LUNAR_FULL.matcher(text.replace(" ", ""));
        if (full.matches()) {
            boolean isLeap = full.group(1) != null;
            int month = LunarCalendarUtil.parseChineseMonth(full.group(2));
            int day = LunarCalendarUtil.parseChineseDay(full.group(3));
            if (month > 0 && day > 0) {
                LocalDate date = LunarCalendarUtil.toSolar(year, month, day, isLeap);
                if (date != null) {
                    return new Resolved(date, SOURCE_LUNAR);
                }
            }
        }

        // 4) 「农历七月十二日至九月十五日」这类区间：取起始日
        int idx = text.indexOf("至");
        if (idx > 0) {
            Resolved start = resolve(null, text.substring(0, idx), year);
            if (start != null) {
                return start;
            }
        }

        // 5) 只精确到月：按该月十五估算（如「农历八月」「农历七八月」取第一个月）
        Matcher monthOnly = LUNAR_MONTH_ONLY.matcher(text.replace(" ", ""));
        if (monthOnly.find()) {
            int month = LunarCalendarUtil.parseChineseMonth(monthOnly.group(1));
            LocalDate date = LunarCalendarUtil.toSolar(year, month, 15, false);
            if (date != null) {
                return new Resolved(date, SOURCE_APPROX);
            }
        }

        return null;
    }

    /** 解析公历字段（实体为 LocalDate，或数据库中的文本写法） */
    private static Resolved parseSolar(Object solarDate, int year) {
        if (solarDate == null) {
            return null;
        }
        // 实体字段为 LocalDate：直接使用，无需年份替换
        if (solarDate instanceof LocalDate date) {
            return new Resolved(date, SOURCE_SOLAR);
        }
        String text = String.valueOf(solarDate).trim();
        if (text.isEmpty() || "null".equals(text)) {
            return null;
        }

        Matcher iso = SOLAR_ISO.matcher(text);
        if (iso.matches()) {
            try {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(iso.group(1)),
                        Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)));
                return new Resolved(date, SOURCE_SOLAR);
            } catch (Exception e) {
                return null;
            }
        }

        Matcher cn = SOLAR_CN.matcher(text);
        if (cn.matches()) {
            try {
                MonthDay md = MonthDay.of(Integer.parseInt(cn.group(1)), Integer.parseInt(cn.group(2)));
                return new Resolved(md.atYear(year), SOURCE_SOLAR);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
