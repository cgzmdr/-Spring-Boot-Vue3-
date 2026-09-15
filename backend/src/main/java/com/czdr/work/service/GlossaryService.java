package com.czdr.work.service;

import com.czdr.work.model.entity.TranslateGlossary;
import com.czdr.work.model.request.GlossaryTermRequest;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 翻译词表（本地零依赖兜底翻译的数据源，存库、后台可维护）
 *
 * @author cz
 */
public interface GlossaryService {

    /**
     * 词表匹配结果（用于后台试译预览与自检）：
     *
     * @param translated    译文（未通过覆盖率门限时为空）
     * @param coverage      命中替换掉的有效字符占比（0~1）
     * @param hits          命中的术语数
     * @param accepted      是否达到覆盖率门限（false 表示应让给兜底提供方）
     */
    record GlossaryMatch(String translated, double coverage, int hits, boolean accepted) {
    }

    /** 按词表逐词翻译并做覆盖率判定（结果见 {@link GlossaryMatch}） */
    GlossaryMatch match(String text, String sourceLocale, String targetLocale);

    /**
     * 按词表逐词翻译：取「目标语言」的词条做最长匹配替换；
     * 没有直接词条时自动用反向词条（如 en→zh 反过来用 zh→en 的词条）。
     *
     * @return 译文；未命中、或覆盖率不达门限时返回 null（由调用方转到兜底提供方）
     */
    String translate(String text, String sourceLocale, String targetLocale);

    /**
     * AI 翻译时把译文里给出的术语沉淀进词表（实时优化词表、提升后续词表命中率）。
     * 只新增、绝不覆盖已有词条；术语必须真实出现在原文中，否则丢弃。
     *
     * @param rawTerms AI 输出的术语区文本（每行「术语=译文」）
     * @return 实际新增条数
     */
    int learnTerms(String sourceLocale, String targetLocale, String sourceText, String rawTerms);

    /** 词表变更后清空缓存，下次翻译重新加载 */
    void refresh();

    /** 当前可用的目标语言及其词条数（供后台展示与自检） */
    Map<String, Integer> locales();

    /** 后台：分页查询词条 */
    EasyPageResult<TranslateGlossary> list(String keyword, String targetLocale, Pageable pageable);

    /** 后台：新增或更新词条（按 源语言+目标语言+术语 唯一） */
    String save(GlossaryTermRequest request, String currentTerm);

    /** 后台：删除词条 */
    void delete(UUID id);

    /** 后台：批量导入「术语=译文」（每行一条），返回 { added, skipped, targetLocale } */
    Map<String, Object> importTerms(String text, String sourceLocale, String targetLocale);

    /** 后台：导出词条（每行一条「术语=译文」，可直接再导入） */
    List<String> exportTerms(String targetLocale);

    /** 后台：统计 { total, enabled, locales } */
    Map<String, Object> stats();
}
