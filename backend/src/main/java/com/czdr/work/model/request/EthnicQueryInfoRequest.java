package com.czdr.work.model.request;

import com.czdr.work.model.more.Population;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 民族列表查询条件
 *
 * @author cz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "民族列表查询条件")
public class EthnicQueryInfoRequest {
    @Schema(description = "地域（东北/西北/西南/中南/东南/内蒙古/其他）")
    private String region;
    @Schema(description = "语系")
    private String languageFamily;
    @Schema(description = "人口范围（populationMin/populationMax）")
    private Population population;
    @Schema(description = "关键词（名称/拼音模糊匹配）")
    private String keyword;
    public Map<String,Object> toMap(){
        Map<String,Object> map = new HashMap<>();
        map.put("region",region);
        map.put("languageFamily",languageFamily);
        map.put("population",population);
        map.put("keyword",keyword);
        return map;
    }
}
