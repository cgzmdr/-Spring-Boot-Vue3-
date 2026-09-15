package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 历史沿革结构化结果（方向 C-2）。
 *
 * <p><b>为什么不是「年表」：</b>实测 56 个民族的 {@code 【历史沿革】} 是编年体散文而非年表——
 * 仅 10/56 行的段落本身按时间排序，346/1121 段（31%）完全没有时间锚点
 * （如藏族的「差巴/堆穷/朗生」农奴等级、蒙古族的「畜牧业经济部门」）。
 * 因此这里**只做可验证的结构化**：把带真实时间锚点的段落抽成时间轴，
 * 其余段落进段落索引，**不推断、不归一化模糊年份**，所有展示文本均取自原文。</p>
 *
 * @param timeline    时间轴：仅收录原文中带具体年份（或明确朝代+年份）的段落，按出现顺序保持原文
 * @param eras        时代分期：按原文自述的朝代/时代词归类，仅保留实际命中的时代
 * @param paragraphs  段落索引：全部段落，供侧栏导航与折叠展示
 * @param timelineCount  时间轴条目数
 * @param eraCount    命中的时代数
 * @param paragraphCount 段落总数
 * @param anchoredRatio 有时间锚点的段落占比（0~1，用于页面如实提示「其余为专题叙述」）
 * @author cz
 */
@Schema(description = "历史沿革结构化结果")
public record EthnicHistoryResource(
        @Schema(description = "时间轴（仅含原文明确年份的段落）") List<TimelineItem> timeline,
        @Schema(description = "时代分期（按原文自述时代词归类）") List<EraGroup> eras,
        @Schema(description = "段落索引（全部段落）") List<Paragraph> paragraphs,
        @Schema(description = "时间轴条目数") int timelineCount,
        @Schema(description = "命中的时代数") int eraCount,
        @Schema(description = "段落总数") int paragraphCount,
        @Schema(description = "有时间锚点的段落占比") double anchoredRatio
) {

    /**
     * 时间轴条目。
     *
     * @param year    原文出现的年份（如 1206）；公元前以负数表示，如 -221
     * @param yearText 年份的原文写法（如「1206年」「公元前221年」），保留原文不做归一化
     * @param text    该段落原文（不截断、不改写）
     * @param index   对应 paragraphs 中的下标，便于前端联动高亮
     */
    @Schema(description = "时间轴条目")
    public record TimelineItem(
            @Schema(description = "原文年份（公元前为负数）") int year,
            @Schema(description = "年份原文写法") String yearText,
            @Schema(description = "段落原文") String text,
            @Schema(description = "段落下标") int index
    ) {
    }

    /**
     * 时代分期。
     *
     * @param name        时代名（先秦/秦汉/魏晋南北朝/隋唐五代/宋辽金西夏/元代/明代/清代/近现代）
     * @param paragraphIndexes 该时代命中的段落下标
     */
    @Schema(description = "时代分期")
    public record EraGroup(
            @Schema(description = "时代名") String name,
            @Schema(description = "段落下标列表") List<Integer> paragraphIndexes
    ) {
    }

    /**
     * 段落。
     *
     * @param index   下标
     * @param text    段落原文
     * @param eras    该段命中的时代名（可能为空：专题叙述段）
     * @param year    该段首个明确年份；无则为 null
     */
    @Schema(description = "段落")
    public record Paragraph(
            @Schema(description = "下标") int index,
            @Schema(description = "段落原文") String text,
            @Schema(description = "命中的时代") List<String> eras,
            @Schema(description = "首个明确年份，无则为 null") Integer year
    ) {
    }
}
