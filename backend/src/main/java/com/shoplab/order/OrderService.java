package com.shoplab.order;

import com.shoplab.auth.User;
import com.shoplab.cart.Cart;
import com.shoplab.cart.CartItem;
import com.shoplab.cart.CartService;
import com.shoplab.common.ApiException;
import com.shoplab.order.OrderDtos.AddressRequest;
import com.shoplab.order.OrderDtos.CardRequest;
import com.shoplab.order.OrderDtos.CreateOrderRequest;
import com.shoplab.order.OrderDtos.OrderDto;
import com.shoplab.pricing.PriceCalculator;
import com.shoplab.pricing.PriceCalculator.Line;
import com.shoplab.pricing.PriceCalculator.Totals;
import com.shoplab.product.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

@Service
@Transactional
public class OrderService {

    /** The test card that is always declined (same as the frontend). */
    public static final String DECLINED_CARD = "4000000000000002";

    private static final java.util.regex.Pattern EXPIRY = java.util.regex.Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");

    private final OrderRepository orders;
    private final CartService cartService;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, CartService cartService) {
        this.orders = orders;
        this.cartService = cartService;
    }

    /**
     * Checkout: turns the cart into an order. Steps:
     * 1) validate card  2) cart must not be empty  3) enough stock  4) payment
     * 5) create order + decrease stock  6) empty the cart.
     * Thanks to @Transactional, if anything fails, NOTHING is saved (no half-created orders).
     */
    public OrderDto placeOrder(User user, CreateOrderRequest req) {
        String cardNumber = null;
        if (req.paymentMethod() == PaymentMethod.CARD) {
            cardNumber = validateCard(req.card());
        }

        Cart cart = cartService.getOrCreate(user);
        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }
        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            if (item.getQuantity() > p.getStock()) {
                throw ApiException.conflict("Only " + p.getStock() + " item(s) of \"" + p.getTitle() + "\" available in stock");
            }
        }
        if (DECLINED_CARD.equals(cardNumber)) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment declined. Please use a different card.");
        }

        AddressRequest a = req.shippingAddress();
        ShippingAddress address = new ShippingAddress(a.fullName().trim(), a.address().trim(), a.city().trim(),
                a.zip().trim(), a.country().trim(), a.phone().trim());
        String last4 = cardNumber == null ? null : cardNumber.substring(12);

        Order order = new Order(newOrderNumber(), user, req.deliveryMethod(), req.paymentMethod(), last4,
                cart.getPromoCode(), address);
        for (CartItem item : cart.getItems()) {
            order.addItem(new OrderItem(order, item.getProduct(), item.getQuantity()));
            item.getProduct().decreaseStock(item.getQuantity());
        }

        List<Line> lines = order.getItems().stream().map(i -> new Line(i.getUnitPrice(), i.getQuantity())).toList();
        Totals t = PriceCalculator.calculate(lines, cart.getPromoCode(), req.deliveryMethod());
        order.setTotals(t.subtotal(), t.discount(), t.shipping(), t.total());

        orders.save(order);
        cart.clear();
        return OrderDto.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(User user) {
        return orders.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream().map(OrderDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(User user, String orderNumber) {
        return OrderDto.from(findOwnOrder(user, orderNumber));
    }

    /** Only PROCESSING orders can be cancelled. The items go back into stock. */
    public OrderDto cancel(User user, String orderNumber) {
        Order order = findOwnOrder(user, orderNumber);
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw ApiException.conflict("Only orders in PROCESSING status can be cancelled");
        }
        order.cancel();
        order.getItems().forEach(i -> i.getProduct().increaseStock(i.getQuantity()));
        return OrderDto.from(order);
    }

    private Order findOwnOrder(User user, String orderNumber) {
        // Someone else's order also gives 404 (not 403): we don't reveal that it exists.
        return orders.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order " + orderNumber + " not found"));
    }

    /** Checks the card like the frontend does. Returns the card number with spaces/dashes removed. */
    private static String validateCard(CardRequest card) {
        Map<String, String> errors = new TreeMap<>();
        if (card == null) {
            errors.put("card", "Card details are required for card payment");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation failed", errors);
        }

        String number = card.number() == null ? "" : card.number().replaceAll("[\\s-]", "");
        if (!number.matches("^\\d{16}$")) {
            errors.put("card.number", "Card number must be 16 digits");
        }

        Matcher m = EXPIRY.matcher(card.expiry() == null ? "" : card.expiry().trim());
        if (!m.matches()) {
            errors.put("card.expiry", "Use the format MM/YY");
        } else {
            YearMonth expiry = YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            if (expiry.isBefore(YearMonth.now())) { // valid until the end of the expiry month
                errors.put("card.expiry", "Your card has expired");
            }
        }

        if (card.cvv() == null || !card.cvv().trim().matches("^\\d{3}$")) {
            errors.put("card.cvv", "CVV must be 3 digits");
        }

        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation failed", errors);
        }
        return number;
    }

    /** Public order id like the frontend's: "SL-" + 6 random digits. */
    private String newOrderNumber() {
        String number;
        do {
            number = "SL-" + (100000 + random.nextInt(900000));
        } while (orders.existsByOrderNumber(number));
        return number;
    }
}
