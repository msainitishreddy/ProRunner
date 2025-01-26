package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.CartDTO;
import com.prorunner.prorunner.dto.CartProductDTO;
import com.prorunner.prorunner.service.CartService;
import com.prorunner.prorunner.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    /**
     * View a cart by ID.
     */
    @Operation(summary = "View a cart by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}")
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> viewCart(@PathVariable Long cartId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size){
        try {
            logger.info("Fetching cart with ID: {}", cartId);
            CartDTO cartDTO = cartService.getCartById(cartId);
            return ResponseEntity.ok(new StandardResponse<>("Cart fetched successfully", cartDTO));
        } catch (Exception e) {
            logger.error("Error fetching the cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Add a product to the cart.
     */
    @Operation(summary = "Add a product to the cart", description = "Add a product to the cart by " +
            "specifying cart ID, user ID, product ID, and quantity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Error adding product to cart")
    })
    @PostMapping("/{cartId}/add")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> addProductToCart(@PathVariable Long cartId,
                                                                      @RequestParam Long userId,
                                                                      @RequestParam Long productId,
                                                                      @RequestParam int quantity){
        try {
            logger.info("Adding product {} with quantity {} to cart {}", productId, quantity, cartId);
            CartDTO updatedCart = cartService.addProductToCart(cartId, userId, productId, quantity);
            return ResponseEntity.ok(new StandardResponse<>("Product added to cart successfully", updatedCart));
        } catch (Exception e) {
            logger.error("Error adding product to cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Remove a product from the cart.
     */
    @Operation(summary = "Remove a product from the cart", description = "Remove a specific product from the cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed from the cart successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or Product not found")
    })
    @DeleteMapping("/{cartId}/remove/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> removeProductFromCart(@PathVariable Long cartId, @PathVariable Long productId){
        try {
            logger.info("Removing product {} from cart {}", productId, cartId);
            CartDTO updatedCart = cartService.removeProductFromCart(cartId, productId);
            return ResponseEntity.ok(new StandardResponse<>("Product removed from the cart successfully", updatedCart));
        } catch (Exception e) {
            logger.error("Error removing product from cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Update the quantity of a product in the cart.
     */
    @Operation(summary = "Update the quantity of a product in the cart", description = "Change the quantity of a specific product in the cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity"),
            @ApiResponse(responseCode = "404", description = "Cart or Product not found")
    })
    @PatchMapping("/{cartId}/update/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> updateProductQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            logger.info("Updating quantity of product {} in cart {} to {}", productId, cartId, quantity);
            CartDTO updatedCart = cartService.updateProductQuantity(cartId, productId, quantity);
            return ResponseEntity.ok(new StandardResponse<>("Quantity updated successfully", updatedCart));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid quantity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StandardResponse<>(e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error updating product quantity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Decrease product quantity in the cart.
     */
    @Operation(summary = "Decrease product quantity in the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product quantity decreased successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity or error updating cart"),
            @ApiResponse(responseCode = "404", description = "Cart or Product not found")
    })
    @PutMapping("/{cartId}/decrease/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<?> decreaseProductQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam int quantity){
        try {
            if (quantity <= 0) {
                return ResponseEntity.badRequest()
                        .body(new StandardResponse<>("Quantity must be greater than 0", null));
            }
            logger.info("Decreasing quantity of product {} in cart {} by {}", productId, cartId, quantity);
            CartDTO updatedCart = cartService.decreaseProductQuantity(cartId, productId, quantity);
            return ResponseEntity.ok(new StandardResponse<>("Product quantity decreased successfully", updatedCart));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid quantity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StandardResponse<>(e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error updating product quantity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Clear the cart.
     */
    @Operation(summary = "Clear the cart", description = "Remove all items from the cart and reset the total price.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "400", description = "Error clearing cart"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/{cartId}/clear")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> clearCart(@PathVariable Long cartId){
        try {
            logger.info("Clearing cart with ID: {}", cartId);
            CartDTO clearedCart = cartService.clearCart(cartId);
            return ResponseEntity.ok(new StandardResponse<>("Cart cleared successfully", clearedCart));
        } catch (RuntimeException e) {
            logger.error("Error clearing the cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    @Operation(summary = "Get the count of products in the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product count fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}/count")
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<Integer>> getCartProductCount(@PathVariable Long cartId) {
        try {
            logger.info("Fetching product count for cart ID: {}", cartId);
            int productCount = cartService.getCartProductCount(cartId);
            return ResponseEntity.ok(new StandardResponse<>("Product count fetched successfully", productCount));
        } catch (RuntimeException e) {
            logger.error("Error fetching product count for cart ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    // get cart products
    @Operation(summary = "Get all products in the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}/products")
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<List<CartProductDTO>>> getCartProducts(@PathVariable Long cartId) {
        try {
            logger.info("Fetching all products in cart ID: {}", cartId);
            List<CartProductDTO> cartProducts = cartService.getCartProducts(cartId);
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", cartProducts));
        } catch (RuntimeException e) {
            logger.error("Error fetching products for cart ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    @Operation(summary = "Remove a product from all carts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed from all carts successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/remove-from-all/{productId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<String>> removeProductFromAllCarts(@PathVariable Long productId) {
        try {
            logger.info("Removing product ID: {} from all carts", productId);
            cartService.removeProductFromAllCarts(productId);
            return ResponseEntity.ok(new StandardResponse<>("Product removed from all carts successfully", null));
        } catch (RuntimeException e) {
            logger.error("Error removing product ID: {} from all carts: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    @Operation(summary = "Add a product to the cart with partial stock if insufficient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added to cart with partial stock"),
            @ApiResponse(responseCode = "404", description = "Product or cart not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/{cartId}/add-partial")
    @PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> addProductToCartWithPartialStock(
            @PathVariable Long cartId,
            @RequestParam Long productId,
            @RequestParam int requestedQuantity) {
        try {
            logger.info("Adding product ID: {} with requested quantity: {} to cart ID: {}", productId, requestedQuantity, cartId);
            CartDTO updatedCart = cartService.addProductToCartWithPartialStock(cartId, productId, requestedQuantity);
            return ResponseEntity.ok(new StandardResponse<>("Product added to cart with partial stock", updatedCart));
        } catch (RuntimeException e) {
            logger.error("Error adding product to cart with partial stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @Operation(summary = "Get or create a cart for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart fetched or created successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<CartDTO>> getOrCreateCart(@PathVariable Long userId) {
        try {
            logger.info("Fetching or creating cart for user ID: {}", userId);
            CartDTO cartDTO = cartService.getOrCreateCart(userId);
            return ResponseEntity.ok(new StandardResponse<>("Cart fetched or created successfully", cartDTO));
        } catch (RuntimeException e) {
            logger.error("Error fetching or creating cart for user ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    /**
     * Merge a guest cart with a user's cart after login.
     */

    @Operation(summary = "Merge two carts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carts merged successfully"),
            @ApiResponse(responseCode = "404", description = "One or both carts not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<StandardResponse<CartDTO>> mergeCarts(
            @RequestParam Long guestCartId,
            @RequestParam Long userCartId) {
        try {
            logger.info("Merging cart {} into cart {}", guestCartId, userCartId);
            CartDTO updatedCart = cartService.mergeCarts(guestCartId, userCartId);
            return ResponseEntity.ok(new StandardResponse<>("Carts merged successfully", updatedCart));
        } catch (Exception e) {
            logger.error("Error merging carts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Error merging carts", null));
        }
    }



}
