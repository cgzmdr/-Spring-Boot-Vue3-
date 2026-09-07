package com.czdr.work.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cz
 */
@Getter
public enum EntityType {
    ART("art"),
    TOPIC("topic"),
    ETHNIC("ethnic"),
    FESTIVAL("festival");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }
    public static Map<String,String> toMap(){
        Map<String,String> map = new HashMap<>();
        for (EntityType entityType : EntityType.values()) {
            map.put(entityType.name(),entityType.value);
        }
        return map;
    }
}
