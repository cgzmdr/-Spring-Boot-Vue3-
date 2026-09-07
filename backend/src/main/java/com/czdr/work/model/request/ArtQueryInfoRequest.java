package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author cz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "艺术列表查询条件")
public class ArtQueryInfoRequest {
    @Schema(description = "艺术类别（music/dance/drama/costume/craft/architecture）")
    private String category;
    @Schema(description = "所属民族 ID")
    private UUID ethnicGroupId;
    @Schema(description = "非遗级别（world/national/provincial）")
    private String intangibleHeritage;
    @Schema(description = "关键词（名称模糊匹配）")
    private String keyword;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("category", category);
        map.put("ethnicGroupId", ethnicGroupId);
        map.put("intangibleHeritage", intangibleHeritage);
        map.put("keyword", keyword);
        return map;
    }
}
