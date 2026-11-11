package com.shoplab.pricing;

import java.util.Map;
import java.util.Optional;

/** The promo codes the shop accepts (same as the frontend). */
public final class PromoCodes {

    public static final String QA10 = "QA10";
    public static final String FREESHIP = "FREESHIP";

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            QA10, "10% off your order",
            FREESHIP, "Free standard shipping");

    private PromoCodes() {
    }

    /** Returns the normalized (trimmed, upper-case) code if it exists. */
    public static Optional<String> normalize(String code) {
        if (code == null) return Optional.empty();
        String normalized = code.trim().toUpperCase();
        return DESCRIPTIONS.containsKey(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    public static String describe(String code) {
        return code == null ? null : DESCRIPTIONS.get(code);
    }
}
