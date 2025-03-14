package com.prorunner.prorunner.service;

import com.prorunner.prorunner.dto.CartDTO;
import com.prorunner.prorunner.dto.CartProductDTO;
import com.prorunner.prorunner.model.Cart;
import com.prorunner.prorunner.model.CartProduct;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.repository.CartProductRepository;
import com.prorunner.prorunner.repository.CartRepository;
import com.prorunner.prorunner.repository.ProductRepository;
import com.prorunner.prorunner.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartProductRepository cartProductRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);


    // Helper methods for mapping
    private CartDTO mapToDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cartDTO.setCartProducts(cart.getCartProducts()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
        cartDTO.setTotalPrice(cart.getTotalPrice());
        return cartDTO;
    }

    private List<CartProductDTO> mapToDTO(List<CartProduct> cartProducts) {
        return cartProducts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CartProductDTO mapToDTO(CartProduct cartProduct){
//        return modelMapper.map(cartProduct,CartProductDTO.class);
        CartProductDTO cartProductDTO = modelMapper.map(cartProduct, CartProductDTO.class);
        cartProductDTO.setImageUrl(cartProduct.getProduct().getImageUrl()); // Make sure the image URL is mapped
        return cartProductDTO;
    }



    // Fetch cart by ID and map to CartDTO class
    public CartDTO getCartById(Long cartId){
        logger.info("Fetching cart with ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));
        return mapToDTO(cart);
    }


    @Transactional
    public CartDTO addProductToCart(String sessionId, Long userId, Long productId, int quantity){

        if ((sessionId == null || sessionId.isEmpty()) && userId == null) {
            throw new IllegalArgumentException("Either sessionId or userId must be provided");
        }

        Cart cart = getOrCreateCartEntity(sessionId, userId);

        if (productId == null) {
            throw new IllegalArgumentException("Product ID must be valid");
        }
        if(quantity <= 0){
            throw new IllegalArgumentException("Provide a valid quantity");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist"));

        if (product.getAvailableStock() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        logger.info("Adding product {} with quantity {} to cart", productId, quantity);
        
        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> new CartProduct(cart, product, 0, product.getPrice()));

        if(cartProduct.getUnitPrice() == null){
            cartProduct.setUnitPrice(product.getPrice());
        }

        cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
        cartProduct.updateSubtotal();
        cartProductRepository.save(cartProduct);

        product.setReservedStock(product.getReservedStock() + quantity);
        productRepository.save(product);

        updateCartTotal(cart);

        return mapToDTO(cart);
    }
    @Transactional
    private Cart getOrCreateCartEntity(String sessionId, Long userId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId)
                    .orElseGet(() -> createUserCart(userId));
        } else if(sessionId != null){
            logger.info("Checking for existing cart with sessionId: {}", sessionId);
            return cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createGuestCart(sessionId));
        }else {
            throw new IllegalArgumentException("Either sessionId or userId must be provided");
        }
    }

    @Transactional
    public CartDTO getOrCreateCart(String sessionId, Long userId){
        Cart cart;

        if(userId !=null){
            cart = cartRepository.findByUserId(userId)
                    .orElseGet(() -> createUserCart(userId));
        } else if(sessionId != null){
            cart = cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createGuestCart(sessionId));
        } else {
            throw new IllegalArgumentException("Either sessionId or userId must be provided...");
        }

        return mapToDTO(cart);
    }

    private Cart createUserCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalPrice(0.0);
        return cartRepository.save(cart);
    }

    private Cart createGuestCart(String sessionId) {

        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setTotalPrice(0.0);
        return cartRepository.save(cart);
    }


    @Transactional
    public CartDTO removeProductFromCart(String sessionId, Long userId, Long productId){

        Cart cart = getOrCreateCartEntity(sessionId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID: "+productId+" does not exist."));
        logger.info("Product found: {}", product.getName());

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart,product)
                .orElseThrow(() -> new RuntimeException("Product not found in the cart"));
        logger.info("CartProduct found: Quantity {} at Price {}", cartProduct.getQuantity(), cartProduct.getUnitPrice());

        cartProductRepository.delete(cartProduct);
        cart.getCartProducts().remove(cartProduct);
        product.setReservedStock(product.getReservedStock()-cartProduct.getQuantity());
        productRepository.save(product);
        updateCartTotal(cart);

        return mapToDTO(cartRepository.save(cart));
    }

    /**
     * Update the quantity of a product in the cart.
     */
    @Transactional
    public CartDTO addProductQuantity(String sessionId, Long userId, Long productId, boolean increment){

        logger.info("Adjusting product quantity (increment: {}) for product {}", increment, productId);
        Cart cart = getOrCreateCartEntity(sessionId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: "+productId));

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart, product)
                .orElseThrow(()->new RuntimeException("Product not found in the cart"));

        int updatedQuantity = cartProduct.getQuantity() + (increment ? 1 : -1);

        if (updatedQuantity <= 0){
            logger.info("Deleting product with ID {} from the cart because the quantity is zero", productId);
            cartProductRepository.delete(cartProduct);
            cart.getCartProducts().remove(cartProduct);
            product.setReservedStock(product.getReservedStock()-cartProduct.getQuantity());
            productRepository.save(product);
        } else {
            cartProduct.setQuantity(updatedQuantity);
            cartProduct.updateSubtotal();
            cartProductRepository.save(cartProduct);
        }
        updateCartTotal(cart);
        return mapToDTO(cartRepository.save(cart));
    }

    @Transactional
    public void removeProductFromAllCarts(Long productId){

        List<CartProduct> cartProducts = cartProductRepository.findByProductId(productId);

        for (CartProduct cartProduct: cartProducts){
            Cart cart = cartProduct.getCart();
            cartProductRepository.delete(cartProduct);
            updateCartTotal(cart);
            cartRepository.save(cart);
        }

    }

    public Page<CartProductDTO> viewCart(Long cartId, int page, int size){
        logger.info("Fetching cart products for cart ID: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));

        Pageable pageable = PageRequest.of(page, size);
        Page<CartProduct> cartProducts = cartProductRepository.findByCart(cart, pageable);

        return cartProducts.map(this::mapToDTO);
    }

    @Transactional
    public CartDTO clearCart(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: "+cartId));

        if(cartProductRepository.countByCart(cart) == 0){
            throw new RuntimeException("Cart is already empty");
        }

        cartProductRepository.deleteAllByCart(cart);
        cart.setTotalPrice(0.0);
        return mapToDTO(cartRepository.save(cart));
    }

    @Transactional
    public List<CartProductDTO> getCartProducts(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: "+cartId));

        return mapToDTO(cartProductRepository.findByCart(cart));
    }

    @Transactional
    private void updateCartTotal(Cart cart) {logger.info("Updating total price for cart ID: {}", cart.getId());

        BigDecimal totalPrice = cartProductRepository.findByCart(cart).stream()
                .map(cartProduct -> BigDecimal.valueOf(cartProduct.getSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP).doubleValue());
        cartRepository.save(cart);
    }


    public int getCartProductCount(Long cartId) {
        logger.info("Fetching product count for cart ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));
        return cart.getCartProducts().stream()
                .mapToInt(CartProduct::getQuantity)
                .sum();
    }


    public CartDTO getOrCreateGuestCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    newCart.setTotalPrice(0.0);
                    return cartRepository.save(newCart);
                });
        return mapToDTO(cart);
    }


    /**
     * Merges the contents of a guest cart into a user's cart after login.
     *
     * @param guestSessionId The session ID of the guest cart.
     * @param userId         The ID of the logged-in user.
     * @return The updated user cart as a DTO.
     */
    @Transactional
    public CartDTO mergeCarts(String guestSessionId, Long userId) {
        logger.info("Merging guest cart with session ID: {} into user cart for user ID: {}", guestSessionId, userId);

        // Fetch the guest cart by session ID
        Cart guestCart = cartRepository.findBySessionId(guestSessionId)
                .orElseThrow(() -> new RuntimeException("Guest cart not found for session: " + guestSessionId));

        // Fetch or create the user cart
        Cart userCart = cartRepository.findByUserId(userId)
                .orElseGet(()->createUserCart(userId));

        if (guestCart == null || guestCart.getCartProducts().isEmpty()) {
            logger.info("No guest cart found or guest cart is empty. Returning the user's cart.");
            return mapToDTO(userCart);
        }
        // Merge guest cart products into user cart
        for (CartProduct guestProduct : guestCart.getCartProducts()) {

            CartProduct userProduct = cartProductRepository.findByCartAndProduct(userCart,guestProduct.getProduct())
                    .orElseGet(()->new CartProduct(userCart,guestProduct.getProduct(),0,guestProduct.getUnitPrice()));

            userProduct.setQuantity(userProduct.getQuantity() + guestProduct.getQuantity());
            userProduct.updateSubtotal();
            cartProductRepository.save(userProduct);
        }
        // Update total price of the user cart
        userCart.setTotalPrice(userCart.getCartProducts().stream()
                .mapToDouble(CartProduct::getSubtotal)
                .sum());

        cartRepository.save(userCart);

        // Delete guest cart
        cartRepository.delete(guestCart);

        logger.info("Guest cart merged successfully into user cart for user ID: {}", userId);

        return mapToDTO(userCart);
    }
    
}
