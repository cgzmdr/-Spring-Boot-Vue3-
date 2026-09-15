package com.czdr.work.service.history;

import com.czdr.work.model.resource.EthnicHistoryResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 历史沿革结构化解析器（方向 C-2）。
 *
 * <p>把 {@code ethnic_group.description} 中的 {@code 【历史沿革】} 小节拆成
 * 「时间轴 / 时代分期 / 段落索引」三部分。纯函数、无 IO，便于单测。</p>
 *
 * <p><b>诚实性约束（本类最重要的设计原则）：</b></p>
 * <ul>
 *   <li>时间轴**只**收录原文写出了具体年份的段落。像「13世纪初」「公元3世纪」「唐宋时」
 *       这类模糊表述**不进入时间轴**——把「13世纪初」写成 1200 年属于编造。</li>
 *   <li>所有输出文本均为原文切片，不做改写、不截断。</li>
 *   <li>朝代归并只依据原文自述的朝代词，不依据年份反推朝代。</li>
 * </ul>
 *
 * @author cz
 */
@Component
public class EthnicHistoryParser {

    /** 小节起始标记（实测 56/56 行均存在且各出现一次） */
    private static final String HISTORY_MARK = "【历史沿革】";
    /** 下一小节标记（历史沿革的结束边界） */
    private static final String NEXT_MARK = "【风俗文化】";

    /**
     * 明确年份：4 位年（1000-2100）或 3 位年（100-999，如「651年」），可带「公元前」前缀。
     * 要求数字后面紧跟「年」，避免把人口数、面积数误当年份。
     */
    private static final Pattern YEAR = Pattern.compile("(公元前)?(\\d{3,4})\\s*年");

    /** 时代词表：顺序即时间先后，用于归并与排序 */
    private static final List<Map.Entry<String, Pattern>> ERAS = List.of(
            Map.entry("先秦", Pattern.compile("(先秦|远古|石器时代|新石器|旧石器|夏朝|商朝|殷商|殷周|西周|东周|春秋|战国|三皇五帝)")),
            Map.entry("秦汉", Pattern.compile("(秦朝|秦代|秦王朝|西汉|东汉|两汉|汉代|汉朝|汉武帝|秦汉)")),
            Map.entry("魏晋南北朝", Pattern.compile("(三国|曹魏|西晋|东晋|魏晋|南北朝|北魏|南朝)")),
            Map.entry("隋唐五代", Pattern.compile("(隋朝|隋代|隋唐|唐朝|唐代|唐王朝|五代十国|五代)")),
            Map.entry("宋辽金西夏", Pattern.compile("(北宋|南宋|宋代|宋朝|辽朝|辽代|契丹国|金朝|金代|女真|西夏|大理国)")),
            Map.entry("元代", Pattern.compile("(元朝|元代|蒙古帝国|成吉思汗|忽必烈)")),
            Map.entry("明代", Pattern.compile("(明朝|明代|明王朝|洪武|永乐)")),
            Map.entry("清代", Pattern.compile("(清朝|清代|清王朝|清初|清末|康熙|乾隆|雍正)")),
            Map.entry("近现代", Pattern.compile("(民国|辛亥革命|鸦片战争|抗日|解放战争|革命战争|新中国成立|中华人民共和国|1949|土地改革|民主改革)"))
    );

    /**
     * 解析历史沿革。
     *
     * @param description 民族详细介绍全文（可能为 null / 不含历史沿革小节）
     * @return 结构化结果；无可解析内容时返回各项为空的结果（而非 null），便于前端统一处理
     */
    public EthnicHistoryResource parse(String description) {
        String section = extractSection(description);
        if (section.isBlank()) {
            return new EthnicHistoryResource(List.of(), List.of(), List.of(), 0, 0, 0, 0d);
        }

        String[] rawParagraphs = section.split("\\R+");
        List<String> texts = new ArrayList<>();
        for (String p : rawParagraphs) {
            String trimmed = p.strip();
            // 过滤过短残片（如误入的孤立标点行）
            if (trimmed.length() > 5) {
                texts.add(trimmed);
            }
        }

        List<EthnicHistoryResource.Paragraph> paragraphs = new ArrayList<>(texts.size());
        List<EthnicHistoryResource.TimelineItem> timeline = new ArrayList<>();
        // 按时代词表的先后顺序收集（而非段落出现顺序）：段落是专题式行文，
        // 时代在正文里并不按时间先后出现，直接按首次出现排序会让「先秦→秦汉→清代→近现代→魏晋」
        // 这种乱序显示到界面上，故按 ERAS 的时间顺序输出。
        Map<String, List<Integer>> eraHits = new LinkedHashMap<>();
        int anchored = 0;

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);

            List<String> eras = new ArrayList<>();
            for (Map.Entry<String, Pattern> era : ERAS) {
                if (era.getValue().matcher(text).find()) {
                    eras.add(era.getKey());
                }
            }
            // 命中即登记，登记顺序由下面的 ERAS 遍历保证为时间顺序
            for (String eraName : eras) {
                eraHits.computeIfAbsent(eraName, k -> new ArrayList<>()).add(i);
            }

            Integer firstYear = null;
            String firstYearText = null;
            Matcher m = YEAR.matcher(text);
            while (m.find()) {
                int value = Integer.parseInt(m.group(2));
                if (m.group(1) != null) {
                    value = -value;
                }
                if (firstYear == null) {
                    firstYear = value;
                    firstYearText = m.group();
                }
                // 时间轴只收「一个段落里的第一个明确年份」，避免同段落多条目重复正文
                break;
            }
            if (firstYear != null) {
                anchored++;
                timeline.add(new EthnicHistoryResource.TimelineItem(firstYear, firstYearText, text, i));
            }

            paragraphs.add(new EthnicHistoryResource.Paragraph(i, text, eras, firstYear));
        }

        // 按 ERAS 的时间顺序输出（而非 map 的插入顺序）：
        // LinkedHashMap 只保证「首次遇到某个时代」的先后，而正文是专题式行文，
        // 先出现的可能是清代、后面才提到秦汉，直接遍历 map 会得到乱序的时代列表。
        List<EthnicHistoryResource.EraGroup> eras = new ArrayList<>();
        for (Map.Entry<String, Pattern> era : ERAS) {
            List<Integer> hits = eraHits.get(era.getKey());
            if (hits != null && !hits.isEmpty()) {
                eras.add(new EthnicHistoryResource.EraGroup(era.getKey(), List.copyOf(hits)));
            }
        }

        double ratio = texts.isEmpty() ? 0d : (double) anchored / texts.size();
        return new EthnicHistoryResource(
                List.copyOf(timeline),
                List.copyOf(eras),
                List.copyOf(paragraphs),
                timeline.size(),
                eras.size(),
                paragraphs.size(),
                Math.round(ratio * 1000d) / 1000d);
    }

    /**
     * 截取【历史沿革】小节正文。
     *
     * <p>用 indexOf 而非正则：小节标记在实测数据中定位明确，且避免正则跨行长文本回溯。
     * 找到起始标记后，向下一个小节标记（【风俗文化】）截断；若缺失则取到文末。</p>
     */
    String extractSection(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        int start = description.indexOf(HISTORY_MARK);
        if (start < 0) {
            return "";
        }
        String rest = description.substring(start + HISTORY_MARK.length());
        int end = rest.indexOf(NEXT_MARK);
        return (end >= 0 ? rest.substring(0, end) : rest).strip();
    }
}
