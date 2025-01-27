package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.OrderDTO;
import com.prorunner.prorunner.model.Order;
import com.prorunner.prorunner.service.OrderService;
import com.prorunner.prorunner.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Place an order", description = "Place an order for a given cart and user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or error placing order")
    })
    @PostMapping("/{cartId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<OrderDTO>> placeOrder(@PathVariable @NotNull Long cartId,
                                                              @RequestParam @NotNull Long userId,
                                                              @RequestParam @NotNull Long addressId){
        try {
            OrderDTO order = orderService.placeOrder(cartId,userId,addressId);
            return ResponseEntity.ok(new StandardResponse<>("Order placed successfully", order));
        } catch (Exception e){
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @Operation(summary = "Get user orders", description = "Fetch all orders for a specific user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders fetched successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isUser(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<StandardResponse<List<OrderDTO>>> getUserOrders(@PathVariable @NotNull Long userId){
        try {
            List<OrderDTO> orders = orderService.getUserOrders(userId);
            return ResponseEntity.ok(new StandardResponse<>("Orders fetched successfully", orders));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @Operation(summary = "Get order by ID", description = "Fetch a specific order by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<StandardResponse<OrderDTO>> getOrderById(@PathVariable @NotNull Long orderId){
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(new StandardResponse<>("Orders fetched successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }
}
