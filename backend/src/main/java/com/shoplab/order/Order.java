package com.shoplab.order;

import com.shoplab.auth.User;
import com.shoplab.pricing.DeliveryMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** "Order" is a reserved word in SQL (ORDER BY), hence the table name "orders". */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** EnumType.STRING stores "PROCESSING", not the number 0. Safer and readable in SQL. */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "promo_code")
    private String promoCode;

    private BigDecimal subtotal;
    private BigDecimal discount;
    @Column(name = "shipping_cost")
    private BigDecimal shippingCost;
    private BigDecimal total;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String orderNumber, User user, DeliveryMethod deliveryMethod, PaymentMethod paymentMethod,
                 String cardLast4, String promoCode, ShippingAddress shippingAddress) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.status = OrderStatus.PROCESSING;
        // PostgreSQL stores time with MICROsecond precision, Java's clock has NANOseconds.
        // Without truncating, POST /api/orders would return ...054530079Z but a later GET ...054530Z:
        // the same order with two different createdAt values (found by OrdersApiTest.getOrderByNumber).
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.deliveryMethod = deliveryMethod;
        this.paymentMethod = paymentMethod;
        this.cardLast4 = cardLast4;
        this.promoCode = promoCode;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void setTotals(BigDecimal subtotal, BigDecimal discount, BigDecimal shippingCost, BigDecimal total) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.shippingCost = shippingCost;
        this.total = total;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public User getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getCardLast4() { return cardLast4; }
    public String getPromoCode() { return promoCode; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getTotal() { return total; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public List<OrderItem> getItems() { return items; }
}
