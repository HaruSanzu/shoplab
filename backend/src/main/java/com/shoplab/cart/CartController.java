package com.shoplab.cart;

import com.shoplab.auth.CurrentUser;
import com.shoplab.auth.User;
import com.shoplab.cart.CartDtos.AddItemRequest;
import com.shoplab.cart.CartDtos.CartDto;
import com.shoplab.cart.CartDtos.PreviewRequest;
import com.shoplab.cart.CartDtos.PromoRequest;
import com.shoplab.cart.CartDtos.UpdateQuantityRequest;
import com.shoplab.pricing.DeliveryMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Every endpoint returns the whole updated cart, so the client always has fresh totals. */
@RestController
@RequestMapping("/api/cart")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "The logged-in user's cart (login required)")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get my cart",
            description = "deliveryMethod (STANDARD by default) changes the shipping cost in the totals, used on the checkout page.")
    public CartDto get(@CurrentUser User user,
                       @RequestParam(defaultValue = "STANDARD") String deliveryMethod) {
        return service.getCart(user, DeliveryMethod.fromJson(deliveryMethod));
    }

    @PostMapping("/preview")
    @SecurityRequirements // = no login needed (overrides the class-level requirement)
    @Operation(summary = "Calculate a guest cart without saving it (no login needed)",
            description = "The website keeps a guest's cart in the browser and asks the server for prices and totals.")
    public CartDto preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request);
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product (adds to the quantity if it is already in the cart)")
    public CartDto addItem(@CurrentUser User user, @Valid @RequestBody AddItemRequest request) {
        return service.addItem(user, request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of a product in the cart")
    public CartDto updateQuantity(@CurrentUser User user, @PathVariable long productId,
                                  @Valid @RequestBody UpdateQuantityRequest request) {
        return service.updateQuantity(user, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the cart")
    public CartDto removeItem(@CurrentUser User user, @PathVariable long productId) {
        return service.removeItem(user, productId);
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart (also removes the promo code)")
    public CartDto clear(@CurrentUser User user) {
        return service.clear(user);
    }

    @PostMapping("/promo")
    @Operation(summary = "Apply a promo code: QA10 or FREESHIP")
    public CartDto applyPromo(@CurrentUser User user, @RequestBody PromoRequest request) {
        return service.applyPromo(user, request.code());
    }

    @DeleteMapping("/promo")
    @Operation(summary = "Remove the promo code")
    public CartDto removePromo(@CurrentUser User user) {
        return service.removePromo(user);
    }
}
