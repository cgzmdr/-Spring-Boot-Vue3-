package com.czdr.work;

import com.czdr.work.model.resource.EthnicHistoryResource;
import com.czdr.work.service.history.EthnicHistoryParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史沿革解析器 · 真实数据校验（方向 C-2）。
 *
 * <p>直接读 psql 导出的 ethnic_group 全文（TSV），对 56 个民族逐一解析并断言：
 * 时间轴条目必须都能在原文里找到对应年份、时代分区不得为空、段落不得丢失。
 * 用真实数据而非构造样例，避免「测试通过但线上数据崩」。</p>
 *
 * <p>运行：gradlew runHistoryCheck（见 build.gradle 中的同名任务）</p>
 */
public final class HistoryParserCheck {

    public static void main(String[] args) throws IOException {
        Path tsv = Path.of(args.length > 0 ? args[0] : "scripts/ethnic-descriptions.tsv");
        if (!Files.exists(tsv)) {
            System.err.println("缺少导出文件：" + tsv.toAbsolutePath());
            System.exit(2);
        }
        List<String[]> rows = readTsv(Files.readString(tsv, StandardCharsets.UTF_8));
        EthnicHistoryParser parser = new EthnicHistoryParser();

        int fails = 0, checks = 0;
        long totalTimeline = 0, totalParagraphs = 0, withTimeline = 0;

        for (String[] row : rows) {
            String name = row[0], desc = row[1];
            EthnicHistoryResource h = parser.parse(desc);

            checks++;
            if (h.paragraphCount() == 0) {
                System.err.println("FAIL " + name + "：未解析出任何段落");
                fails++;
                continue;
            }
            // 1) 每个时间轴条目必须在原文中能找到该年份写法
            for (EthnicHistoryResource.TimelineItem it : h.timeline()) {
                checks++;
                if (!it.text().contains(it.yearText())) {
                    System.err.println("FAIL " + name + "：年份写法不在原文中 -> " + it.yearText());
                    fails++;
                }
                // 2) 段落下标必须合法且指向同一段文本
                checks++;
                if (it.index() < 0 || it.index() >= h.paragraphs().size()
                        || !h.paragraphs().get(it.index()).text().equals(it.text())) {
                    System.err.println("FAIL " + name + "：时间轴 index 与 paragraphs 不一致 -> " + it.index());
                    fails++;
                }
            }
            // 3) 时代分区的段落下标必须落在合法范围
            for (EthnicHistoryResource.EraGroup era : h.eras()) {
                checks++;
                for (Integer idx : era.paragraphIndexes()) {
                    if (idx < 0 || idx >= h.paragraphs().size()) {
                        System.err.println("FAIL " + name + "：时代 " + era.name() + " 下标越界 -> " + idx);
                        fails++;
                    }
                }
            }
            // 4) 计数一致性
            checks++;
            if (h.timelineCount() != h.timeline().size()
                    || h.paragraphCount() != h.paragraphs().size()
                    || h.eraCount() != h.eras().size()) {
                System.err.println("FAIL " + name + "：计数字段与列表长度不一致");
                fails++;
            }
            // 5) 占比区间合法
            checks++;
            if (h.anchoredRatio() < 0d || h.anchoredRatio() > 1d) {
                System.err.println("FAIL " + name + "：anchoredRatio 越界 -> " + h.anchoredRatio());
                fails++;
            }
            // 6) 时代必须按时间先后排列（回归守卫：曾因遍历 map 插入顺序导致乱序）
            checks++;
            List<String> order = List.of("先秦", "秦汉", "魏晋南北朝", "隋唐五代", "宋辽金西夏",
                    "元代", "明代", "清代", "近现代");
            int prev = -1;
            for (EthnicHistoryResource.EraGroup era : h.eras()) {
                int rank = order.indexOf(era.name());
                if (rank < 0 || rank <= prev) {
                    System.err.println("FAIL " + name + "：时代顺序错误 -> " + era.name());
                    fails++;
                    break;
                }
                prev = rank;
            }
            // 7) 时间轴条目内年份必须与 yearText 一致（公元前为负）
            checks++;
            for (EthnicHistoryResource.TimelineItem it : h.timeline()) {
                int digits = Integer.parseInt(it.yearText().replaceAll("\\D", ""));
                boolean bc = it.yearText().contains("公元前");
                if (it.year() != (bc ? -digits : digits)) {
                    System.err.println("FAIL " + name + "：year 与 yearText 不一致 -> " + it.yearText());
                    fails++;
                }
            }

            totalTimeline += h.timelineCount();
            totalParagraphs += h.paragraphCount();
            if (h.timelineCount() > 0) {
                withTimeline++;
            }
        }

        System.out.println("民族行数            : " + rows.size());
        System.out.println("断言数              : " + checks + "，失败 " + fails);
        System.out.println("段落总数            : " + totalParagraphs);
        System.out.println("时间轴条目总数      : " + totalTimeline);
        System.out.println("含时间轴的民族      : " + withTimeline + "/" + rows.size());

        // 6) 空输入健壮性
        EthnicHistoryResource empty = parser.parse(null);
        EthnicHistoryResource noMark = parser.parse("没有任何小节标记的正文");
        if (empty.paragraphCount() != 0 || noMark.paragraphCount() != 0) {
            System.err.println("FAIL 空输入/无标记输入应返回空结构");
            fails++;
        } else {
            System.out.println("空输入与无标记输入  : 均返回空结构 OK");
        }

        System.out.println(fails == 0 ? "\nRESULT: PASS" : "\nRESULT: FAIL(" + fails + ")");
        if (fails > 0) {
            System.exit(1);
        }
    }

    /** 解析 psql \copy CSV(TSV) 导出：每行以「名称<TAB>"正文"」形式出现，正文可含换行 */
    private static List<String[]> readTsv(String raw) {
        String text = raw.replace("\r\n", "\n");
        List<int[]> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^([^\\t\\n]+)\\t\"").matcher(text);
        while (m.find()) {
            starts.add(new int[]{m.end()});
            names.add(m.group(1));
        }
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i)[0];
            int to = i + 1 < starts.size()
                    ? text.lastIndexOf('\n', starts.get(i + 1)[0] - 1)
                    : text.length();
            String body = text.substring(from, Math.max(from, to));
            int close = body.lastIndexOf("\"\n");
            if (close < 0) {
                close = body.lastIndexOf('"');
            }
            if (close >= 0) {
                body = body.substring(0, close);
            }
            out.add(new String[]{names.get(i), body.replace("\"\"", "\"")});
        }
        return out;
    }
}
