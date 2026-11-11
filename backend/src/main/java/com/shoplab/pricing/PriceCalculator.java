package com.shoplab.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The shop's money rules, in ONE place, used by both the cart and checkout.
 * Copied from the frontend's totals() function:
 * <ul>
 *   <li>QA10: 10% off the subtotal (rounded to cents)</li>
 *   <li>Express delivery: always $9.99</li>
 *   <li>Standard delivery: free with FREESHIP, or when (subtotal - discount) >= $50; otherwise $5.99</li>
 *   <li>Empty cart: everything is $0</li>
 * </ul>
 * A pure function (no database, no Spring): perfect for unit tests.
 */
public final class PriceCalculator {

    public static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("50.00");
    public static final BigDecimal STANDARD_SHIPPING = new BigDecimal("5.99");
    public static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("9.99");
    private static final BigDecimal QA10_RATE = new BigDecimal("0.10");

    private PriceCalculator() {
    }

    /** One line of a cart or order. */
    public record Line(BigDecimal unitPrice, int quantity) {
        public BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal total) {
    }

    public static Totals calculate(List<Line> lines, String promoCode, DeliveryMethod delivery) {
        BigDecimal subtotal = money(lines.stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal discount = PromoCodes.QA10.equals(promoCode)
                ? money(subtotal.multiply(QA10_RATE))
                : money(BigDecimal.ZERO);

        BigDecimal shipping;
        if (subtotal.signum() == 0) {
            shipping = BigDecimal.ZERO;
        } else if (delivery == DeliveryMethod.EXPRESS) {
            shipping = EXPRESS_SHIPPING;
        } else if (PromoCodes.FREESHIP.equals(promoCode)
                || subtotal.subtract(discount).compareTo(FREE_SHIPPING_FROM) >= 0) {
            shipping = BigDecimal.ZERO;
        } else {
            shipping = STANDARD_SHIPPING;
        }
        shipping = money(shipping);

        return new Totals(subtotal, discount, shipping, money(subtotal.subtract(discount).add(shipping)));
    }

    /**
     * How much more the customer must spend to get free STANDARD shipping. 0 = it is already free.
     * The website shows it as "Add $X more for FREE shipping".
     */
    public static BigDecimal amountToFreeShipping(Totals totals, String promoCode) {
        if (PromoCodes.FREESHIP.equals(promoCode)) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal missing = FREE_SHIPPING_FROM.subtract(totals.subtotal().subtract(totals.discount()));
        return money(missing.max(BigDecimal.ZERO));
    }

    /** Always 2 decimals, rounding .005 up (like the frontend's Math.round on cents). */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
