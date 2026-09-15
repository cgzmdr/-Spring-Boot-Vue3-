package com.czdr.work.service.impl;

import com.czdr.work.model.resource.FullTextSearchResource;
import com.czdr.work.model.resource.FullTextSearchResource.SearchHit;
import com.czdr.work.service.FullTextSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 全文检索实现（方向 D）。
 *
 * <p><b>为什么用原生 SQL：</b>相关度打分需要把「命中字段 + 命中方式 + 热度 + 时效」
 * 组合成一个表达式，ORM 表达力不够；且必须走 {@code search_document} 上的
 * trigram GIN 索引，用 ORM 拼出来的 SQL 容易退化。此处用 PreparedStatement 全参数化，
 * 不做任何字符串拼接，避免注入。</p>
 *
 * <p><b>三路召回（实测结论驱动）：</b></p>
 * <ol>
 *   <li><b>子串匹配</b>（{@code LIKE}）：中文检索的主力。实测 pg_trgm 对两字中文输入
 *       相似度普遍为 0，仅靠 trigram 会大量漏召回。</li>
 *   <li><b>拼音</b>：全拼（{@code mengguzu} / {@code menggu}）与前缀匹配首选；
 *       首字母缩写（{@code mgz}）仅作低权重补充——实测 56 个民族里有 6 组首字母冲突
 *       （如 hz=汉族/回族、mz=苗族/满族），不能作为唯一依据。</li>
 *   <li><b>trigram 相似度</b>：补充错别字与近似写法（阈值降到 0.2 以提升中文召回）。</li>
 * </ol>
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FullTextSearchServiceImpl implements FullTextSearchService {

    /** 高亮包裹标签（前端按 HTML 渲染；样式由前端 .hl 决定） */
    private static final String HL_OPEN = "<em class=\"hl\">";
    private static final String HL_CLOSE = "</em>";

    /** 摘要截取长度 */
    private static final int SUMMARY_LEN = 160;

    private final DataSource dataSource;

    @Override
    public FullTextSearchResource search(String q, String type, String ethnic, int page, int size) {
        long start = System.currentTimeMillis();
        String keyword = q == null ? "" : q.trim();
        String typeFilter = type == null || type.isBlank() ? "all" : type.trim().toLowerCase(Locale.ROOT);
        String ethnicFilter = ethnic == null || ethnic.isBlank() ? null : ethnic.trim();

        // 空关键词：不做匹配，按热度返回（用于「浏览全部」场景）
        boolean emptyKeyword = keyword.isEmpty();
        // 拼音化检索词：便于匹配全拼与首字母列
        String pyKeyword = com.czdr.work.util.PinyinUtil.full(keyword);
        // 检索词本身可能已是拼音输入（如 mengguzu），此时拼音化结果为空
        String rawLower = keyword.toLowerCase(Locale.ROOT);

        Map<String, Long> facets = new LinkedHashMap<>();
        Map<String, Long> highlights = new LinkedHashMap<>();
        List<SearchHit> hits = new ArrayList<>();
        long total = 0;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        // 供 WHERE 与打分表达式共用
        String likeKw = "%" + keyword + "%";
        String prefixKw = keyword + "%";

        if (!"all".equals(typeFilter)) {
            where.append(" AND doc_type = ? ");
            params.add(typeFilter);
        }
        if (ethnicFilter != null) {
            where.append(" AND ethnic_name = ? ");
            params.add(ethnicFilter);
        }

        if (!emptyKeyword) {
            // 三路召回 + 相关度打分。
            // 打分说明（分值经实测调优，保证「标题精确 > 标题子串 > 拼音全拼 > 首字母 > 正文」）：
            //   title 完全相同 100 / 标题前缀 60 / 标题子串 40
            //   拼音全拼前缀/包含 30 / 首字母精确 20（冲突多，权重低）
            //   trigram 相似度 * 25 / 正文子串 10
            //   热度 log 加权（最多 +8）、时效轻微加权（最多 +4）
            where.append("""
                     AND (
                        title ILIKE ? OR title_en ILIKE ?
                        OR pinyin_full LIKE ? OR pinyin_abbr = ?
                        OR body ILIKE ? OR summary ILIKE ? OR ethnic_name ILIKE ?
                        OR similarity(title, ?) >= 0.2
                     )
                    """);
            params.add(likeKw);            // title
            params.add(likeKw);            // title_en
            params.add("%" + pyKeyword + "%");  // pinyin_full
            params.add(rawLower);          // pinyin_abbr 精确
            params.add(likeKw);            // body
            params.add(likeKw);            // summary
            params.add(likeKw);            // ethnic_name
            params.add(keyword);           // similarity(title, ?)
        }

        String scoreExpr = emptyKeyword
                ? "popularity"
                : """
                  (
                    CASE WHEN lower(title) = lower(?) THEN 100 ELSE 0 END
                  + CASE WHEN title ILIKE ? THEN 60 ELSE 0 END
                  + CASE WHEN title ILIKE ? THEN 40 ELSE 0 END
                  + CASE WHEN title_en ILIKE ? THEN 35 ELSE 0 END
                  + CASE WHEN lower(title_en) = lower(?) THEN 45 ELSE 0 END
                  + CASE WHEN body_en ILIKE ? THEN 15 ELSE 0 END
                  -- 拼音：精确 > 前缀 > 包含。实测若统一按「包含」计分，
                  -- 「蒙古族」(pinyin_full=mengguzu) 会与「河南蒙古族自治县」
                  -- (…mengguzuzizhixian) 同分，导致真正的目标排到自治县之后。
                  + CASE WHEN lower(pinyin_full) = lower(?) THEN 55 ELSE 0 END
                  + CASE WHEN pinyin_full LIKE ? THEN 40 ELSE 0 END
                  + CASE WHEN pinyin_full LIKE ? THEN 30 ELSE 0 END
                  + CASE WHEN lower(pinyin_abbr) = lower(?) THEN 20 ELSE 0 END
                  + CASE WHEN ethnic_name ILIKE ? THEN 18 ELSE 0 END
                  + COALESCE(similarity(title, ?), 0) * 25
                  + CASE WHEN summary ILIKE ? THEN 12 ELSE 0 END
                  + CASE WHEN body ILIKE ? THEN 10 ELSE 0 END
                  + LEAST(COALESCE(popularity,0), 100) * 0.08
                  + CASE WHEN content_at IS NOT NULL
                         THEN GREATEST(0, 4 - EXTRACT(EPOCH FROM (now() - content_at)) / 31536000.0)::numeric
                         ELSE 0 END
                  )
                  """;

        // 统计与分面（与主查询共用同一套条件）
        String countSql = "SELECT count(*) AS n FROM search_document" + where;
        String facetSql = "SELECT doc_type, count(*) AS n FROM search_document" + where + " GROUP BY doc_type ORDER BY n DESC";

        // 主查询
        String listSql = "SELECT *, " + scoreExpr + " AS score FROM search_document" + where
                + " ORDER BY score DESC, popularity DESC, title ASC LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection()) {
            // 总数
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong("n");
                    }
                }
            }
            // 分面
            try (PreparedStatement ps = conn.prepareStatement(facetSql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        facets.put(rs.getString("doc_type"), rs.getLong("n"));
                    }
                }
            }
            // 列表：打分参数在 WHERE 参数之后
            List<Object> listParams = new ArrayList<>();
            if (!emptyKeyword) {
                // 顺序必须与 scoreExpr 中的占位符严格一致
                listParams.add(keyword);              // lower(title) = lower(?)
                listParams.add(prefixKw);             // title ILIKE prefix
                listParams.add("%" + keyword + "%");  // title ILIKE contains
                listParams.add("%" + keyword + "%");  // title_en ILIKE
                listParams.add(keyword);              // lower(title_en) = lower(?) 精确
                listParams.add("%" + keyword + "%");  // body_en ILIKE
                listParams.add(pyKeyword);            // lower(pinyin_full) = lower(?)  精确
                listParams.add(pyKeyword + "%");      // pinyin_full LIKE prefix
                listParams.add("%" + pyKeyword + "%");// pinyin_full LIKE contains
                listParams.add(rawLower);             // pinyin_abbr 精确
                listParams.add("%" + keyword + "%");  // ethnic_name ILIKE
                listParams.add(keyword);              // similarity(title, ?)
                listParams.add("%" + keyword + "%");  // summary ILIKE
                listParams.add("%" + keyword + "%");  // body ILIKE
            }
            listParams.addAll(params);
            listParams.add(size);
            listParams.add(page * size);

            try (PreparedStatement ps = conn.prepareStatement(listSql)) {
                bind(ps, listParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        hits.add(mapHit(rs, keyword, pyKeyword, rawLower, highlights));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("全文检索失败：{}", e.getMessage(), e);
            throw new IllegalStateException("检索服务暂时不可用", e);
        }

        long took = System.currentTimeMillis() - start;
        return new FullTextSearchResource(keyword, total, took, typeFilter, hits, facets, highlights);
    }

    /** 结果集 → 命中项（含高亮与命中方式判定） */
    private SearchHit mapHit(ResultSet rs, String keyword, String pyKeyword, String rawLower,
                             Map<String, Long> highlights) throws SQLException {
        String title = nvl(rs.getString("title"));
        String summary = nvl(rs.getString("summary"));
        String body = nvl(rs.getString("body"));
        String pinyinFull = nvl(rs.getString("pinyin_full"));
        String pinyinAbbr = nvl(rs.getString("pinyin_abbr"));
        String titleEn = nvl(rs.getString("title_en"));
        String ethnic = rs.getString("ethnic_name");

        // 命中方式判定（用于前台提示「按拼音匹配」「按英文匹配」等，也便于说明排序依据）
        String matchBy = "body";
        if (!keyword.isEmpty()) {
            String kwLower = keyword.toLowerCase(Locale.ROOT);
            if (title.toLowerCase(Locale.ROOT).contains(kwLower)) {
                matchBy = "title";
            } else if (!titleEn.isEmpty() && titleEn.toLowerCase(Locale.ROOT).contains(kwLower)) {
                // 英文标题命中：标题本身是中文，前台需据此提示「按英文匹配」而非高亮中文
                matchBy = "titleEn";
            } else if (!pyKeyword.isEmpty() && pinyinFull.contains(pyKeyword)) {
                matchBy = "pinyin";
            } else if (!rawLower.isEmpty() && pinyinAbbr.equalsIgnoreCase(rawLower)) {
                matchBy = "abbr";
            }
        }
        highlights.merge(matchBy, 1L, Long::sum);

        // 摘要优先取 summary，缺失时从 body 截取
        String displaySummary = summary;
        if (displaySummary.isEmpty() && !body.isEmpty()) {
            displaySummary = body.length() > SUMMARY_LEN ? body.substring(0, SUMMARY_LEN) + "…" : body;
        }
        if (displaySummary.length() > SUMMARY_LEN) {
            displaySummary = displaySummary.substring(0, SUMMARY_LEN) + "…";
        }
        // 摘要若不含关键词但正文含，取正文中包含关键词的片段，让高亮可见。
        // 英文检索时同样回退：英文正文里含关键词时给出英文片段，否则会出现
        // 「按英文匹配但摘要里看不到关键词」的困惑。
        if (!keyword.isEmpty() && !displaySummary.contains(keyword)) {
            String bodyEn = nvl(rs.getString("body_en"));
            if (body.contains(keyword)) {
                displaySummary = snippet(body, keyword);
            } else if (bodyEn.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                displaySummary = snippet(bodyEn, keyword);
            }
        }

        return new SearchHit(
                rs.getString("doc_type"),
                String.valueOf(rs.getObject("doc_id")),
                rs.getString("url"),
                title,
                highlight(title, keyword, pyKeyword, pinyinFull, pinyinAbbr, rawLower),
                displaySummary,
                highlight(displaySummary, keyword, pyKeyword, pinyinFull, pinyinAbbr, rawLower),
                ethnic,
                rs.getString("category"),
                rs.getString("region"),
                rs.getString("cover_image"),
                rs.getString("theme_color"),
                rs.getDouble("score"),
                matchBy);
    }

    /**
     * 关键词高亮：把命中的检索词用 &lt;em class="hl"&gt; 包裹。
     *
     * <p>先做 HTML 转义再插标签，避免内容里的 &lt; &gt; 破坏结构或造成 XSS。
     * 拼音命中时对中文原词做标记（用户看到的是中文被高亮，更直观）。</p>
     */
    private String highlight(String text, String keyword, String pyKeyword, String pinyinFull,
                             String pinyinAbbr, String rawLower) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String escaped = escapeHtml(text);
        if (keyword == null || keyword.isEmpty()) {
            return escaped;
        }
        // 直接命中原词（英文检索同样适用：摘要可能已是英文片段）
        String escapedKw = escapeHtml(keyword);
        String out = escaped;
        if (out.toLowerCase(Locale.ROOT).contains(escapedKw.toLowerCase(Locale.ROOT))) {
            out = replaceIgnoreCase(out, escapedKw, HL_OPEN + escapedKw + HL_CLOSE);
        }
        return out;
    }

    /** 大小写不敏感替换（Java String.replace 不支持忽略大小写） */
    private String replaceIgnoreCase(String source, String target, String replacement) {
        StringBuilder sb = new StringBuilder();
        String lowerSource = source.toLowerCase(Locale.ROOT);
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        int from = 0, idx;
        while ((idx = lowerSource.indexOf(lowerTarget, from)) >= 0) {
            sb.append(source, from, idx).append(replacement);
            from = idx + target.length();
        }
        sb.append(source.substring(from));
        return sb.toString();
    }

    /** 截取关键词周边的正文片段（让高亮可见） */
    private String snippet(String text, String keyword) {
        int idx = text.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            return text.length() > SUMMARY_LEN ? text.substring(0, SUMMARY_LEN) + "…" : text;
        }
        int from = Math.max(0, idx - 40);
        int to = Math.min(text.length(), idx + keyword.length() + 100);
        return (from > 0 ? "…" : "") + text.substring(from, to) + (to < text.length() ? "…" : "");
    }

    /** HTML 转义（高亮输出给前端按 HTML 渲染，必须先转义原文） */
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
