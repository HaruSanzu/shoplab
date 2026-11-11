package com.shoplab.cart;

import com.shoplab.auth.User;
import com.shoplab.cart.CartDtos.CartDto;
import com.shoplab.cart.CartDtos.CartItemDto;
import com.shoplab.cart.CartDtos.PreviewItem;
import com.shoplab.cart.CartDtos.PreviewRequest;
import com.shoplab.common.ApiException;
import com.shoplab.pricing.DeliveryMethod;
import com.shoplab.pricing.PriceCalculator;
import com.shoplab.pricing.PriceCalculator.Line;
import com.shoplab.pricing.PriceCalculator.Totals;
import com.shoplab.pricing.PromoCodes;
import com.shoplab.product.Product;
import com.shoplab.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every method is @Transactional: all its database changes are saved together at the end,
 * or none of them if an exception is thrown. Inside a transaction, changes to entities
 * (e.g. item.setQuantity(3)) are saved automatically, no explicit "save" call needed.
 */
@Service
@Transactional
public class CartService {

    public static final int MAX_QUANTITY_PER_ITEM = 10;

    private final CartRepository carts;
    private final ProductService products;

    public CartService(CartRepository carts, ProductService products) {
        this.carts = carts;
        this.products = products;
    }

    public CartDto getCart(User user, DeliveryMethod delivery) {
        return toDto(getOrCreate(user), delivery);
    }

    public CartDto addItem(User user, long productId, int quantity) {
        Cart cart = getOrCreate(user);
        Product product = products.findProduct(productId);
        if (product.getStock() == 0) {
            throw ApiException.conflict("This item is out of stock");
        }
        CartItem existing = cart.findItem(productId).orElse(null);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        checkQuantity(product, newQuantity);

        if (existing == null) {
            cart.addItem(new CartItem(cart, product, quantity));
        } else {
            existing.setQuantity(newQuantity);
        }
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    public CartDto updateQuantity(User user, long productId, int quantity) {
        Cart cart = getOrCreate(user);
        CartItem item = findItemOrThrow(cart, productId);
        checkQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    public CartDto removeItem(User user, long productId) {
        Cart cart = getOrCreate(user);
        cart.removeItem(findItemOrThrow(cart, productId));
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    public CartDto clear(User user) {
        Cart cart = getOrCreate(user);
        cart.clear();
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    public CartDto applyPromo(User user, String code) {
        String normalized = validatePromo(code);
        Cart cart = getOrCreate(user);
        cart.setPromoCode(normalized);
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    public CartDto removePromo(User user) {
        Cart cart = getOrCreate(user);
        cart.setPromoCode(null);
        return toDto(cart, DeliveryMethod.STANDARD);
    }

    /**
     * Calculates a cart that is NOT stored in the database (a guest's cart from the browser).
     * Nothing is saved. The same pricing rules are used, so guests and logged-in users
     * always see identical totals. Stock is not checked here (just like GET /api/cart):
     * it is checked when adding items and when placing the order.
     */
    @Transactional(readOnly = true)
    public CartDto preview(PreviewRequest request) {
        String promo = request.promoCode() == null || request.promoCode().isBlank()
                ? null
                : validatePromo(request.promoCode());
        DeliveryMethod delivery = request.deliveryMethod() == null ? DeliveryMethod.STANDARD : request.deliveryMethod();

        // The same product twice in the list? Add the quantities together.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PreviewItem item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        List<CartItemDto> items = quantities.entrySet().stream()
                .map(e -> toItemDto(products.findProduct(e.getKey()), e.getValue()))
                .toList();
        return buildCart(items, promo, delivery);
    }

    /** Used by OrderService at checkout. */
    public Cart getOrCreate(User user) {
        return carts.findByUserId(user.getId()).orElseGet(() -> carts.save(new Cart(user)));
    }

    private static String validatePromo(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Please enter a promo code");
        }
        return PromoCodes.normalize(code)
                .orElseThrow(() -> ApiException.badRequest("Promo code \"" + code.trim().toUpperCase() + "\" is not valid"));
    }

    private static CartItem findItemOrThrow(Cart cart, long productId) {
        return cart.findItem(productId)
                .orElseThrow(() -> ApiException.notFound("Product " + productId + " is not in the cart"));
    }

    private static void checkQuantity(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw ApiException.conflict("Only " + product.getStock() + " item(s) available in stock");
        }
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw ApiException.conflict("You can buy at most " + MAX_QUANTITY_PER_ITEM + " of the same item");
        }
    }

    private static CartDto toDto(Cart cart, DeliveryMethod delivery) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(i -> toItemDto(i.getProduct(), i.getQuantity()))
                .toList();
        return buildCart(items, cart.getPromoCode(), delivery);
    }

    private static CartItemDto toItemDto(Product p, int quantity) {
        Line line = new Line(p.getPrice(), quantity);
        return new CartItemDto(p.getId(), p.getTitle(), p.getEmoji(), p.getColor(), p.getPrice(), quantity,
                line.total(), p.getStock());
    }

    /** Shared by the stored cart and the guest preview: same rules for everyone. */
    private static CartDto buildCart(List<CartItemDto> items, String promo, DeliveryMethod delivery) {
        List<Line> lines = items.stream().map(i -> new Line(i.unitPrice(), i.quantity())).toList();
        Totals t = PriceCalculator.calculate(lines, promo, delivery);
        int count = items.stream().mapToInt(CartItemDto::quantity).sum();

        return new CartDto(items, count, promo, PromoCodes.describe(promo),
                t.subtotal(), t.discount(), t.shipping(), t.total(),
                PriceCalculator.amountToFreeShipping(t, promo));
    }
}
