package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.config.TranslateProperties;
import com.czdr.work.model.entity.TranslateGlossary;
import com.czdr.work.model.request.GlossaryTermRequest;
import com.czdr.work.service.DescriptionEnBackfillService;
import com.czdr.work.service.GlossaryService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 翻译词表维护接口（后台）：glossary 提供方的数据源，控制哪些文化术语被逐词替换。
 *
 * @author cz
 */
@Tag(name = "翻译词表 Admin · Translate", description = "后台：本地翻译词表（术语 → 译文）的增删改查与导入导出")
@RestController
@RequestMapping("admin/translate")
@RequiredArgsConstructor
public class AdminTranslateController {

    private final GlossaryService glossaryService;
    private final TranslateProperties properties;
    private final DescriptionEnBackfillService descriptionEnBackfillService;

    @Operation(summary = "词表分页查询", description = "keyword 同时匹配术语与译文；targetLocale 过滤目标语言（如 en）")
    @SaCheckPermission("translate:glossary")
    @GetMapping("glossary")
    Result<EasyPageResult<TranslateGlossary>> glossary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetLocale,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(glossaryService.list(keyword, targetLocale, pageable));
    }

    @Operation(summary = "新增 / 更新词条", description = "同一「源语言 + 目标语言 + 术语」唯一；重命名时把原术语放在 currentTerm")
    @SaCheckPermission("translate:glossary")
    @PostMapping("glossary")
    Result<String> saveGlossary(@RequestBody GlossaryTermRequest request,
                               @RequestParam(required = false) @Parameter(description = "原术语（重命名时使用）") String currentTerm) {
        return Result.success(glossaryService.save(request, currentTerm));
    }

    @Operation(summary = "删除词条")
    @SaCheckPermission("translate:glossary")
    @DeleteMapping("glossary/{id}")
    Result<Void> deleteGlossary(@PathVariable String id) {
        glossaryService.delete(UUID.fromString(id));
        return Result.success(null);
    }

    @Operation(summary = "批量导入词条", description = "每行一条「术语=译文」（也支持 → / 制表符 / 全角＝），返回 { added, skipped }")
    @SaCheckPermission("translate:glossary")
    @PostMapping("glossary/import")
    Result<Map<String, Object>> importGlossary(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        String sourceLocale = body == null ? "zh" : body.get("sourceLocale");
        String targetLocale = body == null ? "en" : body.get("targetLocale");
        return Result.success(glossaryService.importTerms(text, sourceLocale, targetLocale));
    }

    @Operation(summary = "导出词条", description = "每行一条「术语=译文」，可直接再导入")
    @SaCheckPermission("translate:glossary")
    @GetMapping(value = "glossary/export", produces = "text/plain;charset=UTF-8")
    String exportGlossary(@RequestParam(required = false) String targetLocale) {
        return String.join("\n", glossaryService.exportTerms(targetLocale));
    }

    @Operation(summary = "词表统计", description = "{ total, enabled, locales }")
    @SaCheckPermission("translate:glossary")
    @GetMapping("glossary/stats")
    Result<Map<String, Object>> glossaryStats() {
        return Result.success(glossaryService.stats());
    }

    @Operation(summary = "刷新词表缓存", description = "词表最长 60 秒自动生效；此接口可让改动立即生效，并返回各目标语言的词条数")
    @SaCheckPermission("translate:glossary")
    @PostMapping("glossary/refresh")
    Result<Map<String, Object>> refreshGlossary() {
        glossaryService.refresh();
        return Result.success(Map.of("refreshed", true, "locales", glossaryService.locales()));
    }

    @Operation(summary = "可选目标语言", description = "返回当前可用的目标语言及词条数（供后台筛选下拉）")
    @SaCheckPermission("translate:glossary")
    @GetMapping("glossary/locales")
    Result<Map<String, Integer>> glossaryLocales() {
        return Result.success(glossaryService.locales());
    }

    @Operation(summary = "词条预览", description = "按当前词表试译一段文本（不写缓存、不调模型）：返回命中条数、覆盖率与是否达到门限；未达门限时该文本会交给兜底提供方")
    @SaCheckPermission("translate:glossary")
    @PostMapping("glossary/preview")
    Result<Map<String, Object>> previewGlossary(@RequestBody Map<String, String> body) {
        String text = body == null ? "" : body.getOrDefault("text", "");
        String sourceLocale = body == null ? "zh" : body.getOrDefault("sourceLocale", "zh");
        String targetLocale = body == null ? "en" : body.getOrDefault("targetLocale", "en");
        GlossaryService.GlossaryMatch match = glossaryService.match(text, sourceLocale, targetLocale);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("original", text);
        result.put("translated", match.translated());
        result.put("hit", match.hits() > 0);
        result.put("hits", match.hits());
        result.put("coverage", Math.round(match.coverage() * 1000) / 1000d);
        result.put("accepted", match.accepted());
        result.put("minCoverage", properties.glossaryMinCoverage());
        return Result.success(result);
    }

    @Operation(summary = "词条列表（不分页，调试用）")
    @SaCheckPermission("translate:glossary")
    @GetMapping("glossary/all")
    Result<List<String>> glossaryAll(@RequestParam(required = false) String targetLocale) {
        return Result.success(glossaryService.exportTerms(targetLocale));
    }

    // ---------------------------------------------------------------- 英文正文回填（方向 C-3）

    @Operation(summary = "英文正文待译统计",
            description = "返回 festival / art / food 三类的 { total, pending, done }，用于查看英文正文补齐进度（不调用模型）")
    @SaCheckPermission("translate:glossary")
    @GetMapping("description-en/pending")
    Result<Map<String, Object>> descriptionEnPending() {
        return Result.success(descriptionEnBackfillService.pending());
    }

    @Operation(summary = "回填英文正文",
            description = "把指定类别的中文正文译成英文并写入 description_en（来源标记 machine）。"
                    + "type 为 festival / art / food；limit 限制本次条数（0=不限）；dryRun=true 只试译不写库。"
                    + "已有人工译文（reviewed/manual）与已机器译过的行会跳过，可安全重复调用续跑。")
    @SaCheckPermission("translate:glossary")
    @PostMapping("description-en/backfill")
    Result<DescriptionEnBackfillService.BatchResult> backfillDescriptionEn(
            @RequestParam @Parameter(description = "内容类别：festival / art / food") String type,
            @RequestParam(defaultValue = "0") @Parameter(description = "本次最多处理条数，0 表示不限") int limit,
            @RequestParam(defaultValue = "false") @Parameter(description = "true 只试译不写库") boolean dryRun) {
        return Result.success(descriptionEnBackfillService.backfill(type, limit, dryRun));
    }
}
