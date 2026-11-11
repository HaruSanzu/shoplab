package com.shoplab.pricing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.shoplab.common.ApiException;

public enum DeliveryMethod {
    STANDARD,
    EXPRESS;

    /** Accept "express", "Express", "EXPRESS" in JSON. */
    @JsonCreator
    public static DeliveryMethod fromJson(String value) {
        for (DeliveryMethod m : values()) {
            if (m.name().equalsIgnoreCase(value)) return m;
        }
        throw ApiException.badRequest("Unknown delivery method '" + value + "'. Allowed: STANDARD, EXPRESS");
    }
}
