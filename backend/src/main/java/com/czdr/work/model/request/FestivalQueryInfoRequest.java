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
@Schema(description = "节日列表查询条件")
public class FestivalQueryInfoRequest {
    @Schema(description = "所属民族 ID")
    private UUID ethnicGroupId;
    @Schema(description = "节日类型（traditional/religious/agricultural）")
    private String type;
    @Schema(description = "公历月份")
    private Integer month;
    @Schema(description = "关键词（节日名称模糊匹配）")
    private String keyword;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ethnicGroupId", ethnicGroupId);
        map.put("type", type);
        map.put("month", month);
        map.put("keyword", keyword);
        return map;
    }
}
