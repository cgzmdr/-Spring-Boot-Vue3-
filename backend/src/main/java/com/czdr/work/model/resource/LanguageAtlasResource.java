package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 民族语言文化（B-3 民族语文专栏）
 * <p>基于 {@code ethnic_group} 的 {@code language_family} / {@code languages} / {@code scripts}
 * 三个字段构建：56 个民族的语言文字信息已全部录入，可直接用于专栏展示。</p>
 *
 * @author cz
 */
@Schema(description = "民族语言文化专栏数据")
public record LanguageAtlasResource(
        @Schema(description = "统计概览") Summary summary,
        @Schema(description = "语系分布（含下辖语族/语言）") List<Family> families,
        @Schema(description = "文字一览（一种文字可被多个民族使用）") List<Script> scripts,
        @Schema(description = "有本民族传统文字的民族数") int groupsWithOwnScript,
        @Schema(description = "使用汉语的民族数") int groupsUsingChinese
) {
    @Schema(description = "专栏统计概览")
    public record Summary(
            @Schema(description = "民族总数") int groupCount,
            @Schema(description = "语言种类数") int languageCount,
            @Schema(description = "文字种类数") int scriptCount,
            @Schema(description = "语系数量") int familyCount
    ) {
    }

    @Schema(description = "一个语系")
    public record Family(
            @Schema(description = "语系名称（原始值，如「汉藏语系·藏缅语族」）") String name,
            @Schema(description = "语系（大语系，如「汉藏语系」）") String family,
            @Schema(description = "语族（如「藏缅语族」，可能为空）") String branch,
            @Schema(description = "使用该语系的民族") List<GroupRef> groups,
            @Schema(description = "该语系下民族人口合计") long population
    ) {
    }

    @Schema(description = "一种文字 / 文字系统")
    public record Script(
            @Schema(description = "文字名称") String name,
            @Schema(description = "使用该文字的民族") List<GroupRef> groups,
            @Schema(description = "是否为该民族的本民族文字（相对「借用汉字」等而言的提示字段）") boolean nativeScript
    ) {
    }

    @Schema(description = "民族简要引用")
    public record GroupRef(
            @Schema(description = "民族 ID") String id,
            @Schema(description = "民族名称") String name,
            @Schema(description = "民族拼音") String pinyin,
            @Schema(description = "主题色") String themeColor,
            @Schema(description = "人口") long population,
            @Schema(description = "该民族使用的语言") List<String> languages,
            @Schema(description = "该民族使用的文字") List<String> scripts
    ) {
    }
}
