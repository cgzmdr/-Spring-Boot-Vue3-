package com.czdr.work.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * JSON 工具：JSONB 数组文本 <-> String[]
 *
 * @author cz
 */
public class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private JsonUtil() {
    }

    /**
     * 将 JSONB 数组列文本（如 {@code ["东北","西北"]}）解析为字符串数组
     */
    public static String[] toStringArray(String json) {
        if (json == null || json.isBlank()) {
            return new String[0];
        }
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE).toArray(new String[0]);
        } catch (JsonProcessingException e) {
            return new String[0];
        }
    }
}
