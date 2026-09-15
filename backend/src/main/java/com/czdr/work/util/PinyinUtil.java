package com.czdr.work.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具（方向 D：全文检索的「拼音 / 首字母」维度）。
 *
 * <p>站点内容以中文为主，用户却常按拼音检索（如输入 {@code mengguzu} 或缩写 {@code mgz} 找「蒙古族」）。
 * 这里在**建立搜索索引时**把中文标题/正文转成两种形式落库，检索时直接对拼音列匹配，
 * 避免每次查询都做转换：</p>
 * <ul>
 *   <li>{@link #full(String)} 全拼：蒙古族 → {@code mengguzu}</li>
 *   <li>{@link #abbr(String)} 首字母：蒙古族 → {@code mgz}</li>
 * </ul>
 *
 * <p>非中文字符（英文、数字、空格）按原样保留并转小写，因此
 * 「维吾尔族 Uyghur」这类混合标题也能正确产出两种形式。</p>
 *
 * @author cz
 */
public class PinyinUtil {

    /** pinyin4j 的输出格式：无声调、小写、ü 用 v 表示（便于用户键盘输入） */
    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private PinyinUtil() {
    }

    /**
     * 全拼（小写、无分隔）。中文转拼音，非中文原样保留。
     *
     * @return 全拼串；输入为空时返回空串
     */
    public static String full(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() * 3);
        for (char c : text.toCharArray()) {
            String[] arr = toPinyin(c);
            if (arr != null && arr.length > 0) {
                sb.append(arr[0]);
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
            // 标点与空白直接丢弃：避免把「·」「（）」等带进拼音串影响匹配
        }
        return sb.toString();
    }

    /**
     * 首字母缩写。中文字取拼音首字母，英文数字原样保留。
     * 例：蒙古族 → {@code mgz}；维吾尔族 → {@code wwez}
     */
    public static String abbr(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            String[] arr = toPinyin(c);
            if (arr != null && arr.length > 0 && !arr[0].isEmpty()) {
                sb.append(Character.toLowerCase(arr[0].charAt(0)));
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /** 单字转拼音（多音字取第一个读音；失败返回 null 而非抛错，保证索引构建不中断） */
    private static String[] toPinyin(char c) {
        try {
            return PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return null;
        }
    }
}
