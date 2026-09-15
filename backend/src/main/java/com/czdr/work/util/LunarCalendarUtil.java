package com.czdr.work.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 中国农历 ↔ 公历换算（1900–2100）。
 * <p>算法：以 1900 年 1 月 31 日（农历庚子年正月初一）为基准，
 * 逐年累加农历年的天数。<b>不引入任何第三方依赖</b>，与项目既有的零依赖风格一致。
 *
 * <p>核心数据 {@link #LUNAR_INFO} 每 20 位二进制描述一个农历年：
 * <ul>
 *   <li>第 0–3 位：闰月月份（1–12，0 表示无闰月）</li>
 *   <li>第 4–15 位：正月到十二月的月大小（1 = 30 天，0 = 29 天）</li>
 *   <li>第 16–19 位：闰月的天数（1 = 30 天，0 = 29 天，无闰月时忽略）</li>
 * </ul>
 *
 * <p>注意：本类只做「农历日期 → 当年公历日期」的正向换算，这正是节日日历所需的场景
 * （节日数据里存的是「农历正月初一」这类表述，需要换算成具体某一年的公历日）。
 *
 * @author cz
 */
public final class LunarCalendarUtil {

    private LunarCalendarUtil() {
    }

    /** 农历数据表：1900–2100 年，每年一个 20 位十六进制数 */
    private static final long[] LUNAR_INFO = {
            0x04bd8L, 0x04ae0L, 0x0a570L, 0x054d5L, 0x0d260L, 0x0d950L, 0x16554L, 0x056a0L, 0x09ad0L, 0x055d2L,
            0x04ae0L, 0x0a5b6L, 0x0a4d0L, 0x0d250L, 0x1d255L, 0x0b540L, 0x0d6a0L, 0x0ada2L, 0x095b0L, 0x14977L,
            0x04970L, 0x0a4b0L, 0x0b4b5L, 0x06a50L, 0x06d40L, 0x1ab54L, 0x02b60L, 0x09570L, 0x052f2L, 0x04970L,
            0x06566L, 0x0d4a0L, 0x0ea50L, 0x06e95L, 0x05ad0L, 0x02b60L, 0x186e3L, 0x092e0L, 0x1c8d7L, 0x0c950L,
            0x0d4a0L, 0x1d8a6L, 0x0b550L, 0x056a0L, 0x1a5b4L, 0x025d0L, 0x092d0L, 0x0d2b2L, 0x0a950L, 0x0b557L,
            0x06ca0L, 0x0b550L, 0x15355L, 0x04da0L, 0x0a5b0L, 0x14573L, 0x052b0L, 0x0a9a8L, 0x0e950L, 0x06aa0L,
            0x0aea6L, 0x0ab50L, 0x04b60L, 0x0aae4L, 0x0a570L, 0x05260L, 0x0f263L, 0x0d950L, 0x05b57L, 0x056a0L,
            0x096d0L, 0x04dd5L, 0x04ad0L, 0x0a4d0L, 0x0d4d4L, 0x0d250L, 0x0d558L, 0x0b540L, 0x0b6a0L, 0x195a6L,
            0x095b0L, 0x049b0L, 0x0a974L, 0x0a4b0L, 0x0b27aL, 0x06a50L, 0x06d40L, 0x0af46L, 0x0ab60L, 0x09570L,
            0x04af5L, 0x04970L, 0x064b0L, 0x074a3L, 0x0ea50L, 0x06b58L, 0x05ac0L, 0x0ab60L, 0x096d5L, 0x092e0L,
            0x0c960L, 0x0d954L, 0x0d4a0L, 0x0da50L, 0x07552L, 0x056a0L, 0x0abb7L, 0x025d0L, 0x092d0L, 0x0cab5L,
            0x0a950L, 0x0b4a0L, 0x0baa4L, 0x0ad50L, 0x055d9L, 0x04ba0L, 0x0a5b0L, 0x15176L, 0x052b0L, 0x0a930L,
            0x07954L, 0x06aa0L, 0x0ad50L, 0x05b52L, 0x04b60L, 0x0a6e6L, 0x0a4e0L, 0x0d260L, 0x0ea65L, 0x0d530L,
            0x05aa0L, 0x076a3L, 0x096d0L, 0x04afbL, 0x04ad0L, 0x0a4d0L, 0x1d0b6L, 0x0d250L, 0x0d520L, 0x0dd45L,
            0x0b5a0L, 0x056d0L, 0x055b2L, 0x049b0L, 0x0a577L, 0x0a4b0L, 0x0aa50L, 0x1b255L, 0x06d20L, 0x0ada0L,
            0x14b63L, 0x09370L, 0x049f8L, 0x04970L, 0x064b0L, 0x168a6L, 0x0ea50L, 0x06b20L, 0x1a6c4L, 0x0aae0L,
            0x092e0L, 0x0d2e3L, 0x0c960L, 0x0d557L, 0x0d4a0L, 0x0da50L, 0x05d55L, 0x056a0L, 0x0a6d0L, 0x055d4L,
            0x052d0L, 0x0a9b8L, 0x0a950L, 0x0b4a0L, 0x0b6a6L, 0x0ad50L, 0x055a0L, 0x0aba4L, 0x0a5b0L, 0x052b0L,
            0x0b273L, 0x06930L, 0x07337L, 0x06aa0L, 0x0ad50L, 0x14b55L, 0x04b60L, 0x0a570L, 0x054e4L, 0x0d160L,
            0x0e968L, 0x0d520L, 0x0daa0L, 0x16aa6L, 0x056d0L, 0x04ae0L, 0x0a9d4L, 0x0a4d0L, 0x0d150L, 0x0f252L,
            0x0d520L,
    };

    /** 基准日：1900-01-31 为农历 1900 年正月初一 */
    private static final LocalDate BASE_DATE = LocalDate.of(1900, 1, 31);

    /** 农历月份中文名（正月 / 二月 …）/ 日中文名 */
    private static final String[] MONTH_NAMES = {
            "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    };
    private static final String[] DAY_NAMES = {
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };

    /** 支持的年份下界（数据表起点） */
    public static final int MIN_YEAR = 1900;
    /** 支持的年份上界 */
    public static final int MAX_YEAR = 1900 + LUNAR_INFO.length - 1;

    /** 农历年 year 的闰月月份；0 表示该年无闰月 */
    private static int leapMonth(int year) {
        return (int) (LUNAR_INFO[year - MIN_YEAR] & 0xf);
    }

    /** 农历年 year 闰月的天数；无闰月返回 0 */
    private static int leapDays(int year) {
        if (leapMonth(year) == 0) {
            return 0;
        }
        return ((LUNAR_INFO[year - MIN_YEAR] & 0x10000L) != 0) ? 30 : 29;
    }

    /** 农历年 year 第 month 月（1–12）的天数 */
    private static int monthDays(int year, int month) {
        return ((LUNAR_INFO[year - MIN_YEAR] & (0x10000L >> month)) != 0) ? 30 : 29;
    }

    /**
     * 农历 year 年 month 月 day 日 → 公历日期。
     *
     * @param year  农历年（如 2026）
     * @param month 农历月（1–12）
     * @param day   农历日（1–30）
     * @param isLeap 是否为闰月
     * @return 对应的公历日期；参数越界时返回 {@code null}
     */
    public static LocalDate toSolar(int year, int month, int day, boolean isLeapMonth) {
        if (year < MIN_YEAR || year > MAX_YEAR || month < 1 || month > 12 || day < 1 || day > 30) {
            return null;
        }
        // 该年此月并非闰月时按平月处理，避免调用方拿到 null 后整体失败
        boolean leap = isLeapMonth && leapMonth(year) == month;

        int offset = 0;
        // 累加基准年到目标年之前的所有整年天数
        for (int y = MIN_YEAR; y < year; y++) {
            offset += yearDays(y);
        }
        // 累加目标年正月至目标月之前的天数（闰月插在对应平月之后）
        for (int m = 1; m < month; m++) {
            offset += monthDays(year, m);
            if (leapMonth(year) == m) {
                offset += leapDays(year);
            }
        }
        // 闰月排在同月平月之后
        if (leap) {
            offset += monthDays(year, month);
        }
        // 月内天数
        int maxDay = leap ? leapDays(year) : monthDays(year, month);
        if (day > maxDay) {
            return null;
        }
        offset += day - 1;

        return BASE_DATE.plusDays(offset);
    }

    /** 农历 year 年的总天数 */
    private static int yearDays(int year) {
        int sum = 348; // 12 个月各按 29 天计
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            if ((LUNAR_INFO[year - MIN_YEAR] & i) != 0) {
                sum += 1;
            }
        }
        return sum + leapDays(year);
    }

    /** 农历月份中文名，如 1 → 「正月」，闰月返回「闰四月」 */
    public static String monthName(int month, boolean isLeap) {
        String name = MONTH_NAMES[Math.max(0, Math.min(11, month - 1))] + "月";
        return isLeap ? "闰" + name : name;
    }

    /** 农历日期中文名，如 5 → 「初五」 */
    public static String dayName(int day) {
        if (day < 1 || day > 30) {
            return "";
        }
        return DAY_NAMES[day - 1];
    }

    /**
     * 中文数字 → 农历日（1–30）。
     * <p>兼容三类写法：
     * <ul>
     *   <li>标准农历日：初一…初十、十一…二十、廿一…廿九、三十</li>
     *   <li>省略「初 / 廿」：九、十五、二十九（数据库中与标准写法混用）</li>
     *   <li>阿拉伯数字：1–30</li>
     * </ul>
     *
     * @return 解析失败返回 -1
     */
    public static int parseChineseDay(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String t = text.trim();
        // 标准写法优先
        for (int i = 1; i <= 30; i++) {
            if (DAY_NAMES[i - 1].equals(t)) {
                return i;
            }
        }
        // 阿拉伯数字
        try {
            int v = Integer.parseInt(t);
            return (v >= 1 && v <= 30) ? v : -1;
        } catch (NumberFormatException ignored) {
            // 继续尝试中文数字
        }
        // 中文数字（「九」「十五」「二十九」「廿九」）
        int bare = parseChineseNumber(t);
        return (bare >= 1 && bare <= 30) ? bare : -1;
    }

    /**
     * 中文数字 → 整数，支持 1–30 的常见写法（一 / 十 / 十五 / 二十 / 廿九 / 二十九）。
     *
     * @return 解析失败返回 -1
     */
    public static int parseChineseNumber(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String t = text.trim().replace("廿", "二十");
        if (t.startsWith("初")) {
            t = t.substring(1);
        }
        if (t.isEmpty()) {
            return -1;
        }
        int digit = chineseDigit(t.charAt(0));
        if (digit <= 0) {
            return -1;
        }
        if (t.length() == 1) {
            // 「十」单独出现表示 10
            return digit == 10 ? 10 : digit;
        }
        // 形如「十五」「二十九」「二十」
        String rest = t.substring(1);
        if (rest.startsWith("十")) {
            int unit = digit * 10;
            String tail = rest.substring(1);
            if (tail.isEmpty()) {
                return unit;
            }
            int tailDigit = chineseDigit(tail.charAt(0));
            return tailDigit > 0 ? unit + tailDigit : -1;
        }
        return -1;
    }

    /** 单个中文数字字符 → 数值；「十」返回 10 */
    private static int chineseDigit(char c) {
        return switch (c) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            case '十' -> 10;
            default -> -1;
        };
    }

    /**
     * 中文数字 → 农历月份（1–12）。
     * <p>兼容：「正」「一」…「十」「十一」「十二」「冬」「腊」，以及
     * 「七八」这种双月写法（取第一个月）、阿拉伯数字。
     *
     * @return 解析失败返回 -1
     */
    public static int parseChineseMonth(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String t = text.trim();
        String[] candidates = {"正", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二", "冬", "腊"};
        int[] values = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 11, 12};
        // 完整匹配优先（避免「十一」被「一」抢先命中）
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].equals(t)) {
                return values[i];
            }
        }
        // 双月写法（如「七八月」「二三月」）：取第一个月
        if (t.length() > 1) {
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i].equals(String.valueOf(t.charAt(0)))) {
                    return values[i];
                }
            }
        }
        try {
            int v = Integer.parseInt(t);
            return (v >= 1 && v <= 12) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 可用于展示的农历月份中文名列表（1–12） */
    public static List<String> monthNames() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            names.add(MONTH_NAMES[i - 1] + "月");
        }
        return names;
    }
}
