package com.shoplab.order;

import com.shoplab.auth.CurrentUser;
import com.shoplab.auth.User;
import com.shoplab.order.OrderDtos.CreateOrderRequest;
import com.shoplab.order.OrderDtos.OrderDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Checkout and order history (login required)")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the current cart",
            description = "Card 4242 4242 4242 4242 succeeds, 4000 0000 0000 0002 is declined (402).")
    public OrderDto placeOrder(@CurrentUser User user, @Valid @RequestBody CreateOrderRequest request) {
        return service.placeOrder(user, request);
    }

    @GetMapping
    @Operation(summary = "My orders, newest first")
    public List<OrderDto> list(@CurrentUser User user) {
        return service.listOrders(user);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "One of my orders, e.g. SL-482913")
    public OrderDto get(@CurrentUser User user, @PathVariable String orderNumber) {
        return service.getOrder(user, orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an order (only while PROCESSING); items return to stock")
    public OrderDto cancel(@CurrentUser User user, @PathVariable String orderNumber) {
        return service.cancel(user, orderNumber);
    }
}
