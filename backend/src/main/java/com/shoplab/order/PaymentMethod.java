package com.shoplab.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.shoplab.common.ApiException;

public enum PaymentMethod {
    /** Credit card: requires card details. */
    CARD,
    /** Cash on delivery: no card needed. */
    COD;

    @JsonCreator
    public static PaymentMethod fromJson(String value) {
        for (PaymentMethod m : values()) {
            if (m.name().equalsIgnoreCase(value)) return m;
        }
        throw ApiException.badRequest("Unknown payment method '" + value + "'. Allowed: CARD, COD");
    }
}
