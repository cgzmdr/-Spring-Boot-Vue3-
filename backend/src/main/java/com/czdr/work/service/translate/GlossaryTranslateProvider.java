package com.czdr.work.service.translate;

import com.czdr.work.service.GlossaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地词表兜底提供方（零依赖、不访问网络）。
 *
 * <p>词表存在数据库表 {@code translate_glossary}，由后台「翻译词表」页维护（迁移 `V7__translate_glossary.sql`
 * 已内置民族名 / 节日 / 非遗 / 美食等文化术语）。按「术语 + 目标语言」做最长匹配替换：
 * CJK 术语按前缀扫描、拉丁术语按词边界匹配、反向词条自动推导。
 * 它不做句法翻译，因此结果会标注为「词表兜底」——这是在未接入模型或模型不可用时保证跨语言阅读最低可用的方案。</p>
 *
 * <p>该目标语言没有词条、或一条都没命中时抛 {@link TranslateException}，由上层降级。</p>
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlossaryTranslateProvider implements TranslateProvider {

    private final GlossaryService glossaryService;

    @Override
    public String name() {
        return "glossary";
    }

    @Override
    public String translate(String text, String sourceLocale, String targetLocale) {
        if (text == null || text.isBlank()) {
            throw new TranslateException("原文为空");
        }
        String translated = glossaryService.translate(text, sourceLocale, targetLocale);
        if (translated == null) {
            throw new TranslateException("本地翻译词表未命中任何词条（目标语言：" + targetLocale + "）");
        }
        return translated;
    }
}
