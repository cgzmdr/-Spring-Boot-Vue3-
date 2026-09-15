package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 图片版权署名（方向 C-4）
 *
 * @author cz
 */
@Schema(description = "图片版权署名")
public record ImageCreditResource(
        @Schema(description = "图片路径") String imagePath,
        @Schema(description = "图片说明（如「苗族 · 封面」）") String caption,
        @Schema(description = "核实状态：verified / unverified / original") String creditStatus,
        @Schema(description = "核实状态显示名") String creditStatusLabel,
        @Schema(description = "作者（未核实时为空）") String author,
        @Schema(description = "许可名称（未核实时为空）") String license,
        @Schema(description = "许可条款链接") String licenseUrl,
        @Schema(description = "来源页面链接") String sourceUrl,
        @Schema(description = "来源站点") String sourceSite,
        @Schema(description = "是否需要署名（CC BY / CC BY-SA 为 true）") Boolean attributionRequired,
        @Schema(description = "备注") String remark,
        @Schema(description = "可直接展示的署名文本（未核实时为空）") String creditLine
) {
}
