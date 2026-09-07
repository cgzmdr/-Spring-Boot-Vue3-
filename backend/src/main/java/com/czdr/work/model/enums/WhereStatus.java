package com.czdr.work.model.enums;

import lombok.Getter;

/**
 * @author cz
 */
@Getter
public enum WhereStatus {
    OR("or"),
    AND("and");
    private final String value;

    WhereStatus(String value) {
        this.value = value;
    }
}
