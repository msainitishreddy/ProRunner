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
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

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
        return cartDTO;
    }

    private List<CartProductDTO> mapToDTO(List<CartProduct> cartProducts) {
        return cartProducts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CartProductDTO mapToDTO(CartProduct cartProduct){
        return modelMapper.map(cartProduct,CartProductDTO.class);
    }

    // Fetch cart by ID and map to CartDTO class
    public CartDTO getCartById(Long cartId){
        logger.info("Fetching cart with ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));
        return mapToDTO(cart);
    }


    @Transactional
    public CartDTO addProductToCart(Long cartId, Long userId, Long productId, int quantity){

        logger.info("Adding product {} with quantity {} to cart {}", productId, quantity, cartId);

        if (cartId == null || userId == null || productId == null || quantity <= 0 ){
            throw new IllegalArgumentException("Invalid cartId, userId, productId, or quantity");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID "+ productId + " does not exist!"));

        if (product.getAvailableStock() == null || product.getAvailableStock() < quantity) {
            throw new RuntimeException("Only " + product.getAvailableStock() + " units available for product: " + product.getName());
        }

        product.setReservedStock(product.getReservedStock()+quantity);
        productRepository.save(product);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart,product)
                .orElseGet(() -> new CartProduct(cart, product, 0, product.getPrice()));

        if(cartProduct.getUnitPrice() == null){
            cartProduct.setUnitPrice(product.getPrice());
        }

        cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
        cartProduct.updateSubtotal();

        cartProductRepository.save(cartProduct);
        updateCartTotal(cart);

        return mapToDTO(cartRepository.save(cart));
    }


    public CartDTO getOrCreateCart(Long userId){
        logger.info("Fetching or creating cart for user ID: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId)); // Assuming `User` has an ID constructor
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setTotalPrice(0.0);
                    return cartRepository.save(newCart);
                });
        return mapToDTO(cart);
    }

    /*if product stock = 10 and user requested for more than 10
    then stock allotted to the user is 10.*/
    @Transactional
    public CartDTO addProductToCartWithPartialStock(Long cartId, Long productId, int quantity) {

        logger.info("Adding product ID: {} with partial stock adjustment to cart ID: {}", productId, cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));


        // Fetch the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist!"));

        // Check stock availability
        int availableStock = product.getAvailableStock();
        if (availableStock <= 0) {
            throw new RuntimeException("Product is out of stock.");
        }
        int finalQuantity = Math.min(quantity, availableStock);

        CartProduct cartProduct = cart.getCartProducts().stream()
                .filter(cp -> cp.getProduct().getId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartProduct newCartProduct = new CartProduct(cart, product, finalQuantity, product.getPrice());
                    cart.getCartProducts().add(newCartProduct);
                    return newCartProduct;
                });

        cartProduct.setQuantity(cartProduct.getQuantity() + finalQuantity);
        cart.setTotalPrice(cart.getCartProducts().stream()
                .mapToDouble(CartProduct::getSubtotal)
                .sum());

        product.setReservedStock(product.getReservedStock() + finalQuantity);
        productRepository.save(product);
        cartRepository.save(cart);

        return mapToDTO(cart);
    }




    @Transactional
    public CartDTO removeProductFromCart(Long cartId, Long productId){

        logger.info("Removing product {} from cart {}", productId, cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: "+cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID: "+productId+" does not exist."));

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart,product)
                .orElseThrow(() -> new RuntimeException("Product not found in the cart"));

        product.setReservedStock(product.getReservedStock()-cartProduct.getQuantity());
        productRepository.save(product);

        cartProductRepository.delete(cartProduct);
        updateCartTotal(cart);

        return mapToDTO(cartRepository.save(cart));
    }

    @Transactional
    public CartDTO updateProductQuantity(Long cartId, Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        // Fetch the cart
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));

        // Find the cart product in the cart
        CartProduct cartProduct = cart.getCartProducts().stream()
                .filter(cp -> cp.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in the cart."));

        // Update the quantity and subtotal
        cartProduct.setQuantity(newQuantity);
        cartProduct.updateSubtotal();

        // Recalculate the total price of the cart
        double updatedTotalPrice = cart.getCartProducts().stream()
                .mapToDouble(CartProduct::getSubtotal)
                .sum();
        cart.setTotalPrice(updatedTotalPrice);

        // Save the updated cart
        Cart updatedCart = cartRepository.save(cart);

        // Convert to CartDTO and return
        return mapToDTO(updatedCart);
    }


    @Transactional
    public CartDTO decreaseProductQuantity(Long cartId, Long productId, int quantity){

        logger.info("Decreasing quantity of product {} in cart {} by {}", productId, cartId, quantity);
        if(cartId == null || productId == null || quantity <=0){
            throw new IllegalArgumentException("Invalid cartId, productId, or quantity.");
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()->new RuntimeException("Cart not found with ID: "+cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist."));

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Product not found in the cart."));

        int updatedQuantity = cartProduct.getQuantity() - quantity;

        if (updatedQuantity > 0){
            cartProduct.setQuantity(updatedQuantity);
            cartProduct.updateSubtotal();
            cartProductRepository.save(cartProduct);
        } else {
            cartProductRepository.delete(cartProduct);
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


    private void updateCartTotal(Cart cart) {

        logger.info("Updating total price for cart ID: {}", cart.getId());
        Double totalPrice = cartProductRepository.findByCart(cart).stream()
                .map(CartProduct::getSubtotal)
                .filter(Objects::nonNull) // Safeguard against null subtotals
                .mapToDouble(Double::doubleValue)
                .sum();

        cart.setTotalPrice(totalPrice != null ? totalPrice : 0.0);
    }


    public int getCartProductCount(Long cartId) {
        logger.info("Fetching product count for cart ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with ID: " + cartId));
        return cart.getCartProducts().stream()
                .mapToInt(CartProduct::getQuantity)
                .sum();
    }


    /**
     * Merges the contents of one cart into another.
     *
     * @param guestCartId The ID of the cart to merge from.
     * @param userCartId The ID of the cart to merge into.
     * @return The updated target cart as a DTO.
     */
    public CartDTO mergeCarts(Long guestCartId, Long userCartId) {
        // Fetch both carts
        Cart sourceCart = cartRepository.findById(guestCartId)
                .orElseThrow(() -> new RuntimeException("Source cart not found with ID: " + guestCartId));
        Cart targetCart = cartRepository.findById(userCartId)
                .orElseThrow(() -> new RuntimeException("Target cart not found with ID: " + userCartId));

        // Merge cart products
        for (CartProduct sourceCartProduct : sourceCart.getCartProducts()) {
            boolean productExists = false;

            // Check if the product already exists in the target cart
            for (CartProduct targetCartProduct : targetCart.getCartProducts()) {
                if (targetCartProduct.getProduct().getId().equals(sourceCartProduct.getProduct().getId())) {
                    // Increment the quantity and update the subtotal
                    targetCartProduct.setQuantity(targetCartProduct.getQuantity() + sourceCartProduct.getQuantity());
                    targetCartProduct.updateSubtotal();
                    productExists = true;
                    break;
                }
            }

            // If the product doesn't exist in the target cart, add it
            if (!productExists) {
                CartProduct newCartProduct = new CartProduct(
                        targetCart,
                        sourceCartProduct.getProduct(),
                        sourceCartProduct.getQuantity(),
                        sourceCartProduct.getUnitPrice()
                );
                targetCart.getCartProducts().add(newCartProduct);
            }
        }

        // Recalculate the total price of the target cart
        targetCart.setTotalPrice(
                targetCart.getCartProducts().stream()
                        .mapToDouble(CartProduct::getSubtotal)
                        .sum()
        );

        // Save the updated target cart
        cartRepository.save(targetCart);

        // Delete the source cart
        cartRepository.delete(sourceCart);

        return mapToDTO(targetCart);
    }



}
