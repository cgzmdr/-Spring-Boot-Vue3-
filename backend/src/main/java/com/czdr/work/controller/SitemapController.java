package com.czdr.work.controller;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.entity.Topic;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * 站点地图（SEO，公开）。
 * <p>
 * 输出已发布内容的 URL 列表（标准 sitemap.xml 格式），供搜索引擎抓取。
 *
 * @author cz
 */
@Tag(name = "站点地图 Sitemap", description = "SEO：输出已发布内容的站点地图（sitemap.xml，公开）")
@RestController
@RequiredArgsConstructor
@RequestMapping("sitemap.xml")
public class SitemapController {
    private final EasyEntityQuery entityQuery;

    /**
     * 站点地图
     * <p>输出已发布民族 / 节日 / 艺术 / 专题内容的 URL 列表（标准 sitemap.xml 格式），供搜索引擎抓取。</p>
     */
    @Operation(summary = "站点地图", description = "输出已发布内容的 XML 站点地图（含民族、节日、艺术、专题 URL），供搜索引擎抓取")
    @GetMapping(produces = "application/xml;charset=UTF-8")
    public String sitemap(HttpServletRequest request) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendEntries(sb, base + "/api/ethnic-groups/",
                entityQuery.queryable(EthnicGroup.class).where(e -> e.status().eq("published")).toList()
                        .stream().map(e -> e.getId().toString()).toList());
        appendEntries(sb, base + "/api/festivals/",
                entityQuery.queryable(Festival.class).where(f -> f.status().eq("published")).toList()
                        .stream().map(f -> f.getId().toString()).toList());
        appendEntries(sb, base + "/api/arts/",
                entityQuery.queryable(Art.class).where(a -> a.status().eq("published")).toList()
                        .stream().map(a -> a.getId().toString()).toList());
        appendEntries(sb, base + "/api/topics/",
                entityQuery.queryable(Topic.class).where(t -> t.status().eq("published")).toList()
                        .stream().map(t -> t.getId().toString()).toList());
        sb.append("</urlset>\n");
        return sb.toString();
    }

    private void appendEntries(StringBuilder sb, String baseUrl, List<String> ids) {
        for (String id : ids) {
            sb.append("  <url><loc>").append(baseUrl).append(id).append("</loc></url>\n");
        }
    }
}
