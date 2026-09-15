package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 后台：民族自治地方保存请求（B 系列）
 *
 * <p>为什么单独建 DTO 而不是直接用实体：{@code autonomous_area.ethnic_groups} 是 jsonb 列，
 * 在实体中以 **JSON 文本**（String）承载；若直接用实体接收请求体，
 * 前端传来的 JSON 数组无法反序列化进 String 字段（会报参数错误）。
 * 因此请求体用 {@code List<String>}，再由服务层序列化为 JSON 文本写入实体 ——
 * 这与 DiscussionServiceImpl 处理 images 数组的做法一致。</p>
 *
 * @author cz
 */
@Data
@Schema(description = "自治地方保存请求")
public class AutonomousAreaSaveRequest {
    @Schema(description = "官方全称")
    private String name;
    @Schema(description = "级别：autonomous_region / autonomous_prefecture / autonomous_county")
    private String level;
    @Schema(description = "冠名的自治民族名称数组")
    private List<String> ethnicGroups;
    @Schema(description = "所属省级行政区")
    private String province;
    @Schema(description = "成立年份（可为空）")
    private Integer establishedYear;
    @Schema(description = "行政中心 / 首府（可为空）")
    private String seat;
}
