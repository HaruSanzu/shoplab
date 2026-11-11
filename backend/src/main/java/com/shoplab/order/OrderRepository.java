package com.shoplab.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Newest first, like the frontend's "Your orders" page. */
    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    /** Only finds the order if it belongs to this user: you can't see other people's orders. */
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    boolean existsByOrderNumber(String orderNumber);
}
