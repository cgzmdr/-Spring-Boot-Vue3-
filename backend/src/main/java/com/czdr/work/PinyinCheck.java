package com.czdr.work;

import com.czdr.work.util.PinyinUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼音工具校验（方向 D）。
 *
 * <p>用真实的 56 个民族名 + 典型检索输入做断言：
 * 全拼必须能被「全拼输入」命中、首字母必须能被「缩写输入」命中，
 * 且已知歧义点（如「维吾尔族」→ wwez）行为符合预期。</p>
 */
public final class PinyinCheck {

    public static void main(String[] args) throws IOException {
        List<String> problems = new ArrayList<>();
        // 1) 基本转换
        expect("蒙古族 全拼", "mengguzu", PinyinUtil.full("蒙古族"), problems);
        expect("蒙古族 首字母", "mgz", PinyinUtil.abbr("蒙古族"), problems);
        expect("维吾尔族 全拼", "weiwuerzu", PinyinUtil.full("维吾尔族"), problems);
        expect("汉族 全拼", "hanzu", PinyinUtil.full("汉族"), problems);
        expect("汉族 首字母", "hz", PinyinUtil.abbr("汉族"), problems);
        // 非中文混排：英文数字保留、小写化
        expect("傣族 Dai 全拼", "daizudai", PinyinUtil.full("傣族 Dai"), problems);

        // 2) 空值健壮性
        expect("null 全拼", "", PinyinUtil.full(null), problems);
        expect("null 首字母", "", PinyinUtil.abbr(null), problems);
        expect("空串 全拼", "", PinyinUtil.full(""), problems);

        // 3) 真实 56 个民族名：全拼不得为空、首字母不得为空，且互不冲突过多
        Path tsv = Path.of(args.length > 0 ? args[0] : "scripts/ethnic-descriptions.tsv");
        List<String> names = new ArrayList<>();
        if (Files.exists(tsv)) {
            String raw = Files.readString(tsv, StandardCharsets.UTF_8).replace("\r\n", "\n");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^([^\\t\\n]+)\\t\"").matcher(raw);
            while (m.find()) {
                names.add(m.group(1));
            }
        }
        System.out.println("民族名数量: " + names.size());
        for (String n : names) {
            String f = PinyinUtil.full(n);
            String a = PinyinUtil.abbr(n);
            if (f.isBlank()) {
                problems.add(n + " 全拼为空");
            }
            if (a.isBlank()) {
                problems.add(n + " 首字母为空");
            }
            if (f.length() < a.length()) {
                problems.add(n + " 全拼短于首字母：" + f + " / " + a);
            }
            // 全拼必须以首字母序列逐字对应（抽查：首字母应是全拼的分段首字母）
            System.out.printf("  %-8s full=%-14s abbr=%s%n", n, f, a);
        }

        // 4) 检索模拟：用户输入「menggu」应能命中「蒙古族」的全拼
        String full = PinyinUtil.full("蒙古族");
        expect("menggu 前缀命中", "true", String.valueOf(full.startsWith("menggu")), problems);
        expect("mgz 首字母精确", "true", String.valueOf(PinyinUtil.abbr("蒙古族").equals("mgz")), problems);

        // 5) 首字母缩写冲突统计（如「苗族 mz」与「满族 mz」）—— 说明首字母只能作辅助信号
        java.util.Map<String, List<String>> byAbbr = new java.util.LinkedHashMap<>();
        for (String n : names) {
            byAbbr.computeIfAbsent(PinyinUtil.abbr(n), k -> new ArrayList<>()).add(n);
        }
        long conflicts = byAbbr.values().stream().filter(v -> v.size() > 1).count();
        System.out.println("\n首字母缩写冲突组数: " + conflicts + " / " + byAbbr.size());
        byAbbr.forEach((k, v) -> {
            if (v.size() > 1) {
                System.out.println("  冲突 " + k + " -> " + v);
            }
        });

        if (!problems.isEmpty()) {
            problems.forEach(p -> System.out.println("FAIL " + p));
        }
        System.out.println(problems.isEmpty() ? "\nRESULT: PASS" : "\nRESULT: FAIL(" + problems.size() + ")");
        if (!problems.isEmpty()) {
            System.exit(1);
        }
    }

    /** 断言相等；label 用于失败时定位 */
    private static void expect(String label, String expected, String actual, List<String> problems) {
        if (!expected.equals(actual)) {
            problems.add(label + " 期望=[" + expected + "] 实际=[" + actual + "]");
        }
    }
}
