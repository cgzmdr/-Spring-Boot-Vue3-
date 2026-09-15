package com.czdr.work.service.impl;

import com.czdr.work.comment.convert.EthnicConvert;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.resource.LanguageAtlasResource;
import com.czdr.work.service.EthnicService;
import com.czdr.work.service.LanguageAtlasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 民族语言文化专栏实现（B-3）
 *
 * <p>数据来源：{@code ethnic_group} 表的三个字段 ——
 * {@code language_family}（语系，如「汉藏语系·藏缅语族」）、
 * {@code languages}（JSONB 数组）、{@code scripts}（JSONB 数组）。
 * 56 个民族的这三个字段均已录入，因此本专栏无需新增数据表。</p>
 *
 * <p>注意：语言文字是文化事实而非可穷尽统计，「使用汉语」等计数仅反映本库
 * 已录入的数据，页面上需明确标注口径，避免被误读为官方统计。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class LanguageAtlasServiceImpl implements LanguageAtlasService {

    /** 判定「本民族传统文字」时的排除项：以下文字属借用或跨境通用文字 */
    private static final Set<String> NON_NATIVE_SCRIPTS = Set.of(
            "汉字", "阿拉伯文", "拉丁文", "斯拉夫文", "藏文（借用）"
    );

    private final EthnicService ethnicService;

    @Override
    public LanguageAtlasResource atlas() {
        List<EthnicGroup> groups = ethnicService.findAll();

        // —— 语系分组：key 为 language_family 原始值 ——
        Map<String, List<EthnicGroup>> byFamily = new LinkedHashMap<>();
        for (EthnicGroup g : groups) {
            String fam = g.getLanguageFamily();
            if (fam == null || fam.isBlank()) {
                fam = "未分类";
            }
            byFamily.computeIfAbsent(fam, k -> new ArrayList<>()).add(g);
        }

        List<LanguageAtlasResource.Family> families = byFamily.entrySet().stream()
                .map(e -> {
                    // 「汉藏语系·藏缅语族」拆成 语系 / 语族 便于前端分层展示
                    String[] parts = e.getKey().split("·", 2);
                    String family = parts[0].trim();
                    String branch = parts.length > 1 ? parts[1].trim() : null;
                    long population = e.getValue().stream().mapToLong(EthnicGroup::getPopulation).sum();
                    List<LanguageAtlasResource.GroupRef> refs = e.getValue().stream()
                            .sorted(Comparator.comparingLong(EthnicGroup::getPopulation).reversed())
                            .map(this::toRef)
                            .toList();
                    return new LanguageAtlasResource.Family(e.getKey(), family, branch, refs, population);
                })
                .sorted(Comparator.comparingLong(LanguageAtlasResource.Family::population).reversed())
                .toList();

        // —— 文字一览：一种文字可能被多个民族使用 ——
        Map<String, List<EthnicGroup>> byScript = new LinkedHashMap<>();
        Set<String> allLanguages = new LinkedHashSet<>();
        for (EthnicGroup g : groups) {
            for (String lang : EthnicConvert.parseStringList(g.getLanguages())) {
                if (lang != null && !lang.isBlank()) {
                    allLanguages.add(lang.trim());
                }
            }
            Set<String> seen = new LinkedHashSet<>();
            for (String sc : EthnicConvert.parseStringList(g.getScripts())) {
                if (sc == null || sc.isBlank()) {
                    continue;
                }
                String name = sc.trim();
                if (!seen.add(name)) {
                    continue;
                }
                byScript.computeIfAbsent(name, k -> new ArrayList<>()).add(g);
            }
        }

        List<LanguageAtlasResource.Script> scripts = byScript.entrySet().stream()
                .map(e -> new LanguageAtlasResource.Script(
                        e.getKey(),
                        e.getValue().stream()
                                .sorted(Comparator.comparingLong(EthnicGroup::getPopulation).reversed())
                                .map(this::toRef)
                                .toList(),
                        !NON_NATIVE_SCRIPTS.contains(e.getKey())))
                // 使用民族多的文字排前面，其次按名称
                .sorted(Comparator
                        .comparingInt((LanguageAtlasResource.Script s) -> s.groups().size()).reversed()
                        .thenComparing(LanguageAtlasResource.Script::name))
                .toList();

        // —— 统计口径 ——
        long groupsWithOwnScript = groups.stream()
                .filter(g -> EthnicConvert.parseStringList(g.getScripts()).stream()
                        .anyMatch(s -> s != null && !s.isBlank() && !NON_NATIVE_SCRIPTS.contains(s.trim())))
                .count();

        long groupsUsingChinese = groups.stream()
                .filter(g -> EthnicConvert.parseStringList(g.getLanguages()).stream()
                        .anyMatch(l -> l != null && l.contains("汉语")))
                .count();

        LanguageAtlasResource.Summary summary = new LanguageAtlasResource.Summary(
                groups.size(),
                allLanguages.size(),
                byScript.size(),
                (int) families.stream().map(LanguageAtlasResource.Family::family).distinct().count()
        );

        return new LanguageAtlasResource(
                summary,
                families,
                scripts,
                (int) groupsWithOwnScript,
                (int) groupsUsingChinese
        );
    }

    private LanguageAtlasResource.GroupRef toRef(EthnicGroup g) {
        return new LanguageAtlasResource.GroupRef(
                g.getId().toString(),
                g.getName(),
                g.getPinyin(),
                g.getThemeColor(),
                g.getPopulation(),
                EthnicConvert.parseStringList(g.getLanguages()),
                EthnicConvert.parseStringList(g.getScripts())
        );
    }
}
