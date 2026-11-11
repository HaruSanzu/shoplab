package com.shoplab.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoplab.pricing.DeliveryMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * Body of POST /api/orders. The products come from the user's cart, not from this body.
     * {@code @Valid} on a nested object means "validate the fields inside it too".
     */
    public record CreateOrderRequest(
            @NotNull(message = "Delivery method is required")
            DeliveryMethod deliveryMethod,

            @NotNull(message = "Payment method is required")
            PaymentMethod paymentMethod,

            @NotNull(message = "Shipping address is required")
            @Valid
            AddressRequest shippingAddress,

            /* Required only when paymentMethod = CARD (checked in OrderService). */
            CardRequest card
    ) {
    }

    /** Rules and messages copied from the frontend checkout form. */
    public record AddressRequest(
            @NotBlank(message = "Please enter your full name")
            @Size(min = 2, max = 100, message = "Please enter your full name")
            String fullName,

            @NotBlank(message = "Address is required")
            @Size(max = 200, message = "Address is too long")
            String address,

            @NotBlank(message = "City is required")
            @Size(max = 100, message = "City is too long")
            String city,

            @NotBlank(message = "ZIP code is required")
            @Pattern(regexp = "^\\s*$|^\\d{5}$", message = "ZIP code must be 5 digits")
            String zip,

            @NotBlank(message = "Please select a country")
            @Size(max = 60, message = "Country is too long")
            String country,

            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\s*$|^\\+?[\\d\\s-]{7,15}$", message = "Enter a valid phone number")
            String phone
    ) {
    }

    /** number: 16 digits (spaces/dashes allowed), expiry: MM/YY, cvv: 3 digits. */
    public record CardRequest(String number, String expiry, String cvv) {
    }

    public record OrderItemDto(Long productId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record AddressDto(String fullName, String address, String city, String zip, String country, String phone) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderDto(
            String orderNumber,
            OrderStatus status,
            Instant createdAt,
            DeliveryMethod deliveryMethod,
            PaymentMethod paymentMethod,
            String cardLast4,
            String promoCode,
            List<OrderItemDto> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal total,
            AddressDto shippingAddress
    ) {
        public static OrderDto from(Order o) {
            ShippingAddress a = o.getShippingAddress();
            return new OrderDto(o.getOrderNumber(), o.getStatus(), o.getCreatedAt(), o.getDeliveryMethod(),
                    o.getPaymentMethod(), o.getCardLast4(), o.getPromoCode(),
                    o.getItems().stream().map(i -> new OrderItemDto(i.getProduct().getId(), i.getProductTitle(),
                            i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList(),
                    o.getSubtotal(), o.getDiscount(), o.getShippingCost(), o.getTotal(),
                    new AddressDto(a.getFullName(), a.getAddress(), a.getCity(), a.getZip(), a.getCountry(), a.getPhone()));
        }
    }
}
