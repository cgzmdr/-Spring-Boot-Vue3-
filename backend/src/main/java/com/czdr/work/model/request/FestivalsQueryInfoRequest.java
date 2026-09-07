package com.czdr.work.model.request;

import com.czdr.work.model.enums.FestivalsType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "节日列表查询条件")
public class FestivalsQueryInfoRequest {
    @Schema(description = "所属民族 ID")
    private String ethnicGroupId;
    @Schema(description = "节日类型（traditional/religious/agricultural）")
    private FestivalsType type;
    @Schema(description = "公历月份")
    private String month;
}
