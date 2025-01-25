package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.model.Cart;
import com.prorunner.prorunner.model.CartProduct;
import com.prorunner.prorunner.service.CartService;
import com.prorunner.prorunner.util.StandardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @GetMapping("/{cartId}")
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<Cart>> viewCart(@PathVariable Long cartId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size){
        try {
            logger.info("Fetching cart with ID: {}", cartId);
            Page<CartProduct> cartProducts = cartService.viewCart(cartId,page,size);

            Cart cart =cartService.getCartById(cartId);

            Map<String, Object> response = new HashMap<>();
            response.put("cartProducts", cartProducts.getContent());
            response.put("totalPrice", cartService.getCartById(cartId).getTotalPrice());
            response.put("pageable", cartProducts.getPageable());

            return ResponseEntity.ok(new StandardResponse<>("Cart fetched successfully", cart));

        } catch (Exception e){
            logger.error("Error fetching the cart: {}", e.getMessage(),e);
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(),null));
        }
    }

    @PostMapping("/{cartId}/add")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> addProductToCart(@PathVariable Long cartId, @RequestParam Long userId, @RequestParam Long productId, @RequestParam int quantity){
        try{
            logger.info("Adding Product {} with quantity {} to the cart {} ", productId,quantity,cartId);
            Cart updatedCart = cartService.addProductToCart(cartId,userId, productId,quantity);
            return ResponseEntity.ok(new StandardResponse<>("Product added to cart", updatedCart));
        } catch (Exception e){
            logger.error("Error adding product to cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(),null));
        }
    }

    @DeleteMapping("/{cartId}/remove/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> removeProductFromCart(@PathVariable Long cartId, @PathVariable Long productId){
        try{
            logger.info("Removing product {} from cart {}", productId, cartId);
            Cart updatedCart = cartService.removeProductFromCart(cartId,productId);
            return ResponseEntity.ok(new StandardResponse<>("Product removed from the cart successfully", updatedCart));
        } catch (Exception e){
            logger.error("Error removing the product from the cart: {}",e.getMessage(),e );
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(),null));
        }
    }

    @PutMapping("/{cartId}/decrease/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<?> decreaseProductQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam int quantity){
        try {
            if(quantity <= 0){
                return ResponseEntity.badRequest().body("Qunatity must be greater than 0 ");
            }
            logger.info("Decreasing quantity of product {} in cart {} by {}, productId, cartId, quantity");
            Cart updatedCart = cartService.decreaseProductQuantity(cartId,productId,quantity);

            return ResponseEntity.ok(updatedCart);
        } catch (Exception e){
            logger.error("Error decreasing product quantity: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{cartId}/clear")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> clearCart(@PathVariable Long cartId){
        try {
            Cart clearedCart = cartService.clearCart(cartId);
            return ResponseEntity.ok(new StandardResponse<>("Cart cleared successfully", clearedCart));
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().body(new StandardResponse<>("Error: "+e.getMessage(),null));
        }
        catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new StandardResponse<>("Error: "+e.getMessage(),null));
        }
    }

}
