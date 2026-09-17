package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.resource.EthnicBriefResource;
import com.czdr.work.model.resource.EthnicInfoDetailedResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cz
 */
public class EthnicConvert {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    public static EthnicQueryInfoResource toInfoModel(EthnicGroup entity){
        return toInfoModel(entity, null);
    }

    /**
     * 列表项转换（带关联指标）。
     *
     * @param metrics 关联内容计数；为 null 时全部按 0 输出
     *                （用于「不需要指标的旧调用点」，避免调用方被迫构造空对象）
     */
    public static EthnicQueryInfoResource toInfoModel(EthnicGroup entity,
                                                      com.czdr.work.model.more.BasicMetrics metrics){
        var m = metrics == null ? com.czdr.work.model.more.BasicMetrics.EMPTY : metrics;
        return new EthnicQueryInfoResource(
                entity.getId().toString(),
                entity.getName(),
                entity.getPinyin(),
                entity.getPopulation(),
                parseStringArray(entity.region),
                entity.getLanguageFamily(),
                entity.getSummary(),
                entity.getCoverImage(),
                entity.getThemeColor(),
                m.artCount(),
                m.festivalCount(),
                m.foodCount(),
                m.customCount(),
                m.locationCount(),
                m.personCount(),
                m.autonomousAreaCount(),
                m.populationRank()
        );
    }
    //EthnicBriefResource（全家福互动墙：id/name/themeColor/coverImage）
    public static EthnicBriefResource toBriefModel(EthnicGroup entity){
        return new EthnicBriefResource(
                entity.getId().toString(),
                entity.getName(),
                entity.getThemeColor(),
                entity.getCoverImage()
        );
    }

    //EthnicInfoDetailedResource
    public static EthnicInfoDetailedResource toInfoDetailedModel(EthnicGroup entity){
        return toInfoDetailedModel(entity, null);
    }

    /**
     * @param history 历史沿革结构化结果（方向 C-2）；由调用方用 {@code EthnicHistoryParser} 解析后传入，
     *                为 null 时返回空结构，前端据此隐藏时间轴区块。
     */
    public static EthnicInfoDetailedResource toInfoDetailedModel(EthnicGroup entity,
                                                                com.czdr.work.model.resource.EthnicHistoryResource history){
        return toInfoDetailedModel(entity, history, null);
    }

    /**
     * 详情转换（带关联信息）。
     *
     * @param related 关联人物 / 自治地方 / 人口排名；为 null 时按空列表与 0 输出。
     *                由 {@code EthnicServiceImpl#relatedOf} 计算，避免在转换器里反向依赖 service。
     */
    public static EthnicInfoDetailedResource toInfoDetailedModel(EthnicGroup entity,
                                                                com.czdr.work.model.resource.EthnicHistoryResource history,
                                                                com.czdr.work.model.more.EthnicRelated related){
        var r = related == null ? com.czdr.work.model.more.EthnicRelated.EMPTY : related;
        return new EthnicInfoDetailedResource(
                entity.getId().toString(),
                entity.getSlug(),
                entity.getName(),
                entity.getNameEn(),
                entity.getSelfName(),
                entity.getPinyin(),
                entity.getPopulation(),
                entity.getLanguageFamily(),
                parseStringArray(entity.getRegion()),
                parseStringArray(entity.getLanguages()),
                parseStringArray(entity.getScripts()),
                parseStringArray(entity.getReligion()),
                entity.getSummary(),
                entity.getSummaryEn(),
                entity.getDescription(),
                entity.getDescriptionEn(),
                entity.getCoverImage(),
                entity.getThemeColor(),
                parseStringArray(entity.getTags()),
                entity.getStatus(),
                entity.getOrderNum(),
                "",
                "",
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCustoms(),
                entity.getLocations(),
                entity.getFoods(),
                entity.getFestivals(),
                entity.getArts(),
                history,
                r.persons(),
                r.autonomousAreas(),
                r.populationRank(),
                r.populationTotal()
        );
    }
    /**
     * JSONB 数组列在实体中以 JSON 文本存储（如 {@code ["东北","西北"]}），
     * 此处解析为字符串数组供接口返回。
     */
    public static String[] parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return new String[0];
        }
        try {
            List<String> list = OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
            return list.toArray(new String[0]);
        } catch (JsonProcessingException e) {
            return new String[0];
        }
    }

    /** 同上，但返回 {@code List}（供统计聚合等需要遍历的场景使用） */
    public static List<String> parseStringList(String json) {
        return new ArrayList<>(List.of(parseStringArray(json)));
    }
}
