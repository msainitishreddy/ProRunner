package com.prorunner.prorunner.service;

import com.prorunner.prorunner.model.Cart;
import com.prorunner.prorunner.model.CartProduct;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.repository.CartProductRepository;
import com.prorunner.prorunner.repository.CartRepository;
import com.prorunner.prorunner.repository.ProductRepository;
import com.prorunner.prorunner.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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



    public Cart getCartById(Long cartId){

        return cartRepository.findById(cartId).orElseThrow(()->
                new RuntimeException("Cart with ID: "+cartId + " not found."));
    }

    public Cart getOrCreateCart(Long cartId, Long userId){
        return cartRepository.findById(cartId).orElseGet(()->{
           Cart newCart = new Cart();
           newCart.setTotalPrice(0.0);
           newCart.setUser(userRepository.findById(userId)
                   .orElseThrow(() -> new RuntimeException("User not found with ID: "+userId)));
           return cartRepository.save(newCart);
        });
    }

    @Transactional
    public Cart addProductToCart(Long cartId, Long userId, Long productId, int quantity){

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

        Cart cart = getOrCreateCart(cartId,userId);

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart,product)
                .orElseGet(() -> new CartProduct(cart, product, 0, product.getPrice()));

        if(cartProduct.getUnitPrice() == null){
            cartProduct.setUnitPrice(product.getPrice());
        }

        cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
        cartProduct.updateSubtotal();

        cartProductRepository.save(cartProduct);
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }

    /*if product stock = 10 and user requested for more than 10
    then stock allotted to the user is 10.*/

    @Transactional
    public Cart addProductToCartWithPartialStock(Long cartId, Long userId, Long productId, int quantity) {
        if (cartId == null || userId == null || productId == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid cartId, userId, productId, or quantity");
        }

        // Fetch the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist!"));

        // Check stock availability
        int availableStock = product.getAvailableStock();
        int quantityToAdd = Math.min(quantity, availableStock);

        // Reserve stock
        product.setReservedStock(product.getReservedStock() + quantityToAdd);
        productRepository.save(product);

        // Get or create cart
        Cart cart = getOrCreateCart(cartId, userId);

        // Add or update cart product
        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> new CartProduct(cart, product, 0, product.getPrice()));

        cartProduct.setQuantity(cartProduct.getQuantity() + quantityToAdd);
        cartProduct.updateSubtotal();
        cartProductRepository.save(cartProduct);

        // Update cart total
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }




    @Transactional
    public Cart removeProductFromCart(Long cartId, Long productId){
        Cart cart = getCartById(cartId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID: "+productId+" does not exist."));

        CartProduct cartProduct = cartProductRepository.findByCartAndProduct(cart,product)
                .orElseThrow(() -> new RuntimeException("Product not found in the cart"));

        product.setReservedStock(product.getReservedStock()-cartProduct.getQuantity());
        productRepository.save(product);

        cartProductRepository.delete(cartProduct);
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart decreaseProductQuantity(Long cartId, Long productId, int quantity){
        if(cartId == null || productId == null || quantity <=0){
            throw new IllegalArgumentException("Invalid cartId, productId, or quantity.");
        }

        Cart cart = getCartById(cartId);
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

        return cartRepository.save(cart);
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

    public Page<CartProduct> viewCart(Long cartId, int page, int size){
        Cart cart = getCartById(cartId);

        Pageable pageable = PageRequest.of(page, size);
        return cartProductRepository.findByCart(cart, pageable);
    }

    @Transactional
    public Cart clearCart(Long cartId) {
        Cart cart = getCartById(cartId);


        if(cartProductRepository.countByCart(cart) == 0){
            throw new RuntimeException("Cart is already empty");
        }

        cartProductRepository.deleteAllByCart(cart);
        cart.setTotalPrice(0.0);
        return cartRepository.save(cart);
    }

    @Transactional
    public List<CartProduct> getCartProducts(Long cartId) {
        Cart cart = getCartById(cartId);
        return cartProductRepository.findByCart(cart);
    }


    private void updateCartTotal(Cart cart) {
        Double totalPrice = cartProductRepository.findByCart(cart).stream()
                .map(CartProduct::getSubtotal)
                .filter(Objects::nonNull) // Safeguard against null subtotals
                .mapToDouble(Double::doubleValue)
                .sum();

        cart.setTotalPrice(totalPrice != null ? totalPrice : 0.0);
    }


    public long getCartProductCount(Long cartId) {
        Cart cart = getCartById(cartId);
        return cartProductRepository.countByCart(cart);
    }


}
