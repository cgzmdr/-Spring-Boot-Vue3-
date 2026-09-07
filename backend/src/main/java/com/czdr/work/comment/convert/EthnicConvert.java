package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.EthnicGroup;
import com.czdr.work.model.resource.EthnicBriefResource;
import com.czdr.work.model.resource.EthnicInfoDetailedResource;
import com.czdr.work.model.resource.EthnicQueryInfoResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @author cz
 */
public class EthnicConvert {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    public static EthnicQueryInfoResource toInfoModel(EthnicGroup entity){
        return new EthnicQueryInfoResource(
                entity.getId().toString(),
                entity.getName(),
                entity.getPinyin(),
                entity.getPopulation(),
                parseStringArray(entity.region),
                entity.getLanguageFamily(),
                entity.getSummary(),
                entity.getCoverImage(),
                entity.getThemeColor()
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
                entity.getArts()
        );
    }
    /**
     * JSONB 数组列在实体中以 JSON 文本存储（如 {@code ["东北","西北"]}），
     * 此处解析为字符串数组供接口返回。
     */
    private static String[] parseStringArray(String json) {
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
}
