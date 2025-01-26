package com.prorunner.prorunner.service;

import com.prorunner.prorunner.model.*;
import com.prorunner.prorunner.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartProductRepository cartProductRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Transactional
    public Order placeOrder(Long cartId, Long userId, Long addressId){

        Address shippingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found."));


        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new RuntimeException("Cart not found."));

        if(cart.getCartProducts().isEmpty()){
            throw new RuntimeException("Cannot place order as cart found empty.");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setShippingAddress(shippingAddress);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setOrderItems(new ArrayList<>());  // initializing the order items....

        Double totalPrice = 0.0;


        for (CartProduct cartProduct:new ArrayList<>(cart.getCartProducts())){
            Product product = cartProduct.getProduct();

            if (product.getStock() < cartProduct.getQuantity()){
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            // Deduct stock
            product.setStock(product.getStock() - cartProduct.getQuantity());
            productRepository.save(product);

            //Create and add orderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartProduct.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(cartProduct.getQuantity()*product.getPrice());

            // Add order item to the order
            order.getOrderItems().add(orderItem);
            // Total price calculations
            totalPrice +=orderItem.getSubtotal();
        }

        // Setting TotalPrice of the order....
        order.setTotalPrice(totalPrice);

        //clear cart products and update cart
        cartProductRepository.deleteAllByCart(cart);
        cart.getCartProducts().clear(); // Clear in-memory references
        cart.setTotalPrice(0.0); // setting the cart total price to 0 after emptying the cart.
        cartRepository.save(cart);

        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
