package com.czdr.work.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @提及解析：从用户内容中提取被提及的昵称。
 *
 * 约定：`@昵称`，昵称允许中英文、数字、下划线、连字符与中点，长度 1~32；
 * 以空白或标点结束。仅做候选提取，是否真实存在由服务层按昵称查库确定。
 *
 * @author cz
 */
public final class MentionParser {

    /** 昵称字符集：字母（含中文等 Unicode 字母）、数字、_ - · */
    private static final Pattern PATTERN = Pattern.compile("@([\\p{L}\\p{N}_\\-·]{1,32})");
    private static final int MAX_MENTIONS = 10;

    private MentionParser() {
    }

    /** 提取提及的昵称（去重、保持出现顺序、最多 10 个） */
    public static List<String> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find() && names.size() < MAX_MENTIONS) {
            String name = matcher.group(1);
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * 昵称能否作为 `@提及` 的完整目标（全字符落在允许集内、长度 1~32）。
     * <p>供联想接口过滤候选：昵称含空格 / emoji / 标点的用户无法被正确解析，因此不提示。</p>
     */
    public static boolean isMentionable(String nickname) {
        return nickname != null && !nickname.isBlank() && PATTERN.matcher("@" + nickname).matches();
    }
}
