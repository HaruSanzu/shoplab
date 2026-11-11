package com.shoplab.cart;

import com.shoplab.auth.User;
import com.shoplab.cart.CartDtos.CartDto;
import com.shoplab.cart.CartDtos.CartItemDto;
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

import java.util.List;

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

    public CartDto getCart(User user) {
        return toDto(getOrCreate(user));
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
        return toDto(cart);
    }

    public CartDto updateQuantity(User user, long productId, int quantity) {
        Cart cart = getOrCreate(user);
        CartItem item = findItemOrThrow(cart, productId);
        checkQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return toDto(cart);
    }

    public CartDto removeItem(User user, long productId) {
        Cart cart = getOrCreate(user);
        cart.removeItem(findItemOrThrow(cart, productId));
        return toDto(cart);
    }

    public CartDto clear(User user) {
        Cart cart = getOrCreate(user);
        cart.clear();
        return toDto(cart);
    }

    public CartDto applyPromo(User user, String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Please enter a promo code");
        }
        String normalized = PromoCodes.normalize(code)
                .orElseThrow(() -> ApiException.badRequest("Promo code \"" + code.trim().toUpperCase() + "\" is not valid"));
        Cart cart = getOrCreate(user);
        cart.setPromoCode(normalized);
        return toDto(cart);
    }

    public CartDto removePromo(User user) {
        Cart cart = getOrCreate(user);
        cart.setPromoCode(null);
        return toDto(cart);
    }

    /** Used by OrderService at checkout. */
    public Cart getOrCreate(User user) {
        return carts.findByUserId(user.getId()).orElseGet(() -> carts.save(new Cart(user)));
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

    private static CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            Line line = new Line(p.getPrice(), i.getQuantity());
            return new CartItemDto(p.getId(), p.getTitle(), p.getEmoji(), p.getPrice(), i.getQuantity(),
                    line.total(), p.getStock());
        }).toList();

        List<Line> lines = items.stream().map(i -> new Line(i.unitPrice(), i.quantity())).toList();
        Totals t = PriceCalculator.calculate(lines, cart.getPromoCode(), DeliveryMethod.STANDARD);
        int count = items.stream().mapToInt(CartItemDto::quantity).sum();

        return new CartDto(items, count, cart.getPromoCode(), PromoCodes.describe(cart.getPromoCode()),
                t.subtotal(), t.discount(), t.shipping(), t.total());
    }
}
