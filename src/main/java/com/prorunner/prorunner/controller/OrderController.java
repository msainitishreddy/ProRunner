package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.model.Order;
import com.prorunner.prorunner.service.OrderService;
import com.prorunner.prorunner.util.StandardResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/{cartId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<Order>> placeOrder(@PathVariable @NotNull Long cartId,
                                                              @RequestParam @NotNull Long userId,
                                                              @RequestParam @NotNull Long addressId){
        try {
            Order order = orderService.placeOrder(cartId,userId,addressId);
            return ResponseEntity.ok(new StandardResponse<>("Order placed successfully", order));
        } catch (Exception e){
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isUser(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<StandardResponse<List<Order>>> getUserOrders(@PathVariable @NotNull Long userId){
        try {
            List<Order> orders = orderService.getUserOrders(userId);
            return ResponseEntity.ok(new StandardResponse<>("Orders fetched successfully", orders));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<StandardResponse<Order>> getOrderById(@PathVariable @NotNull Long orderId){
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(new StandardResponse<>("Orders fetched successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }
}
