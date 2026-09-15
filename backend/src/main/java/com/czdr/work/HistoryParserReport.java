package com.czdr.work;

import com.czdr.work.model.resource.EthnicHistoryResource;
import com.czdr.work.service.history.EthnicHistoryParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 输出每个民族的结构化摘要，便于人工抽查（方向 C-2） */
public final class HistoryParserReport {

    public static void main(String[] args) throws IOException {
        Path tsv = Path.of(args.length > 0 ? args[0] : "scripts/ethnic-descriptions.tsv");
        String raw = Files.readString(tsv, StandardCharsets.UTF_8).replace("\r\n", "\n");
        List<int[]> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^([^\\t\\n]+)\\t\"").matcher(raw);
        while (m.find()) {
            starts.add(new int[]{m.end()});
            names.add(m.group(1));
        }
        EthnicHistoryParser parser = new EthnicHistoryParser();
        var out = new StringBuilder();
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i)[0];
            int to = i + 1 < starts.size() ? raw.lastIndexOf('\n', starts.get(i + 1)[0] - 1) : raw.length();
            String body = raw.substring(from, Math.max(from, to));
            int close = body.lastIndexOf("\"\n");
            if (close < 0) close = body.lastIndexOf('"');
            if (close >= 0) body = body.substring(0, close);
            EthnicHistoryResource h = parser.parse(body.replace("\"\"", "\""));
            out.append(String.format("%-8s 段落%3d 时间轴%3d 时代%d [%s] 锚定%.0f%%%n",
                    names.get(i), h.paragraphCount(), h.timelineCount(), h.eraCount(),
                    h.eras().stream().map(EthnicHistoryResource.EraGroup::name).reduce((a, b) -> a + "," + b).orElse("-"),
                    h.anchoredRatio() * 100));
        }
        Files.writeString(Path.of("scripts/ethnic-history-report.txt"), out.toString(), StandardCharsets.UTF_8);
        System.out.println("written scripts/ethnic-history-report.txt");
    }
}
