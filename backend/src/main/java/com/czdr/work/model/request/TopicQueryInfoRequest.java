package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 专题列表查询条件
 *
 * @author cz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "专题列表查询条件")
public class TopicQueryInfoRequest {
    @Schema(description = "关键词（专题标题模糊匹配）")
    private String keyword;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("keyword", keyword);
        return map;
    }
}
