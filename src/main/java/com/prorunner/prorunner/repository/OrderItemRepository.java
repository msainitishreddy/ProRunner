package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
