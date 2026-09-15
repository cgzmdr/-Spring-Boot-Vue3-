package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 民族分布地图点位（一个聚居地 = 一个点）
 * <p>经纬度取自 {@code ethnic_location}，供 C 端地图视图渲染气泡 / 散点。</p>
 *
 * @author cz
 */
@Schema(description = "民族分布地图点位（聚居地）")
public record EthnicMapPointResource(
        @Schema(description = "聚居地 ID") String id,
        @Schema(description = "所属民族 ID") String ethnicGroupId,
        @Schema(description = "民族名称") String ethnicGroupName,
        @Schema(description = "民族 URL 标识") String ethnicGroupSlug,
        @Schema(description = "民族主题色") String themeColor,
        @Schema(description = "省级行政区") String province,
        @Schema(description = "城市 / 地区") String city,
        @Schema(description = "经度") Double longitude,
        @Schema(description = "纬度") Double latitude,
        @Schema(description = "聚居地说明") String description
) {
}
