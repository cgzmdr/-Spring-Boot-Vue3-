package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 后台：传统体育项目保存请求（B 系列）
 *
 * <p>{@code traditional_sport.ethnic_origins} / {@code sub_events} 均为 jsonb 列，
 * 实体中以 JSON 文本承载；请求体改用 {@code List<String>} 由服务层序列化，
 * 避免 JSON 数组无法写入 String 字段导致的参数错误。</p>
 *
 * @author cz
 */
@Data
@Schema(description = "传统体育项目保存请求")
public class TraditionalSportSaveRequest {
    @Schema(description = "项目名称")
    private String name;
    @Schema(description = "类别：ball / water / strength / accuracy / speed / martial / equestrian / gymnastics / swing")
    private String category;
    @Schema(description = "相关民族名称数组")
    private List<String> ethnicOrigins;
    @Schema(description = "项目描述")
    private String description;
    @Schema(description = "主要器材（可为空）")
    private String equipment;
    @Schema(description = "场地规格（可为空）")
    private String venue;
    @Schema(description = "参赛人数说明（可为空）")
    private String teamSize;
    @Schema(description = "首次成为运动会竞赛项目的年份（可为空）")
    private Integer firstEventYear;
    @Schema(description = "子项名称数组")
    private List<String> subEvents;
    @Schema(description = "相关非遗项目名（可为空）")
    private String heritageLink;
}
