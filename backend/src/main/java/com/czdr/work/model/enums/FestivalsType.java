package com.czdr.work.model.enums;

import lombok.Getter;

/**
 * @author cz
 */

@Getter
public enum FestivalsType {
    TRADITIONAL("traditional"),
    RELIGIOUS("religious"),
    AGRICULTURAL("agricultural");
    private final String value;

    FestivalsType(String value) {
        this.value = value;
    }

}
