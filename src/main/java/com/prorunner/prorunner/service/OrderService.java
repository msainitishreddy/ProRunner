package com.prorunner.prorunner.service;

import com.prorunner.prorunner.dto.OrderDTO;
import com.prorunner.prorunner.dto.OrderItemDTO;
import com.prorunner.prorunner.model.*;
import com.prorunner.prorunner.repository.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;


    public OrderDTO mapToDTO(Order order){
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        orderDTO.setOrderItems(order.getOrderItems().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
        return orderDTO;
    }

    public OrderItemDTO mapToDTO(OrderItem orderItem){
        return modelMapper.map(orderItem, OrderItemDTO.class);
    }

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);


    @Transactional
    public OrderDTO placeOrder(Long cartId, Long userId, Long addressId){

        logger.info("Placing order for user ID: {}, cart ID: {}", userId, cartId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new RuntimeException("Cart not found."));
        Address shippingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));


        if(cart.getCartProducts().isEmpty()){
            throw new RuntimeException("Cannot place order as cart found empty.");
        }

        // Initialize order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setStatus("Placed");
        order.setOrderItems(new ArrayList<>());  // initializing the order items....

        Double totalPrice = 0.0;

        // Processing each product in the cart...
        for (CartProduct cartProduct:new ArrayList<>(cart.getCartProducts())){
            Product product = cartProduct.getProduct();

            // check if product stock is sufficient for placing an order...
            if (product.getStock() < cartProduct.getQuantity()){
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            // Deduct stock from the product stock...
            product.setStock(product.getStock() - cartProduct.getQuantity());
            productRepository.save(product);

            //Create and add orderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartProduct.getQuantity());
            orderItem.setUnitPrice(cartProduct.getUnitPrice());
            orderItem.setSubtotal(cartProduct.getQuantity()*product.getPrice());

            // Add order item to the order
            order.getOrderItems().add(orderItem);
            // Total price calculations
            totalPrice +=orderItem.getSubtotal();
        }

        // Setting TotalPrice of the order....
        order.setTotalPrice(totalPrice);

        // saving the order...
        orderRepository.save(order);

        //clear cart products and update cart
        cartProductRepository.deleteAllByCart(cart);
        cart.setCartProducts(new ArrayList<>()); // Clear in-memory references
        cart.setTotalPrice(0.0); // setting the cart total price to 0 after emptying the cart.
        cartRepository.save(cart);

        logger.info("Order placed successfully for user ID: {}", userId);
        return mapToDTO(order);
    }

    public List<OrderDTO> getUserOrders(Long userId) {
        logger.info("Fetching orders for user ID: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        return orderRepository.findById(orderId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

    }
}
