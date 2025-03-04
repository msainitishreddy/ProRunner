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
    public ResponseEntity<StandardResponse<CartDTO>> viewCart(
                                                     @PathVariable Long cartId,
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
    @PostMapping("/add")
    //@PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> addProductToCart(
                                                                      @RequestParam(required = false) String sessionId,
                                                                      @RequestParam(required = false) Long userId,
                                                                      @RequestParam(name = "productId") Long productId,
                                                                      @RequestParam(name = "quantity") int quantity){
        try {
            System.out.println("Received request: sessionId = " + sessionId + ", userId = " + userId +
                    ", productId = " + productId + ", quantity = " + quantity);
            logger.info("Adding product {} with quantity {} to cart", productId, quantity);
            if((sessionId == null || sessionId.isEmpty()) && userId == null){
                throw new IllegalArgumentException("Either sessionId or userId must be provide...");
            }
            CartDTO updatedCart = cartService.addProductToCart(sessionId, userId, productId, quantity);
            return ResponseEntity.ok(new StandardResponse<>("Product added to cart successfully", updatedCart));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }catch (Exception e) {
            logger.error("Error adding product to cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("An unexpected error occurred", null));
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
    @DeleteMapping("/remove/{productId}")
    //@PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<?>> removeProductFromCart(@RequestParam(required = false) String sessionId,
                                                                     @RequestParam(required = false) Long userId,
                                                                     @PathVariable Long productId){
        try {
            if((sessionId == null || sessionId.isEmpty()) && userId == null){
                throw new IllegalArgumentException("Either sessionId or userId must be provided");
            }
            logger.info("Removing product {} from cart {}", productId, userId);
            CartDTO updatedCart = cartService.removeProductFromCart(sessionId, userId, productId);
            return ResponseEntity.ok(new StandardResponse<>("Product removed from the cart successfully", updatedCart));
        } catch (RuntimeException e) {
            logger.error("Error removing product from cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        } catch (Exception e){
            logger.error("Unexpected error: ",e.getMessage(),e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("An unexpected error occurred", null));
        }
    }

    /**
     * Update the quantity of a product in the cart.
     */
    @Operation(summary = "Update product quantity in the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product quantity updated successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or Product not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PatchMapping("/quantity/{productId}")
    //@PreAuthorize("hasAuthority('USER') or @securityService.isCartOwner(#cartId)")
    public ResponseEntity<StandardResponse<CartDTO>> addProductQuantity(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @PathVariable Long productId,
            @RequestParam boolean increment){
        try {
            logger.info("Received request to update the quantity for product with ID :{} with increment: {}",productId, increment);
            if((sessionId == null || sessionId.isEmpty()) && userId == null){
                throw new IllegalArgumentException("Either sessionId or userId must be provided");
            }
            CartDTO updatedCart = cartService.addProductQuantity(sessionId, userId, productId, increment);
            String action = increment ? "incremented":"decremented";
            return ResponseEntity.ok(new StandardResponse<>("Product Quantity "+action+" successfully",updatedCart));
        } catch (IllegalArgumentException e){
            logger.error("Invalid input: {}", e.getMessage(),e);
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(),null));
        } catch (Exception e){
            logger.error("Error updating the product quantity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("An unexpected error occurred", null));
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


    @Operation(summary = "Get or create a cart for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart fetched or created successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<CartDTO>> getOrCreateCart(
            @PathVariable(required = false) String sessionId,
            @PathVariable(required = false) Long userId) {
        try {
            logger.info("Fetching or creating cart for user ID: {}", userId);
            CartDTO cartDTO = cartService.getOrCreateCart(sessionId,userId);
            return ResponseEntity.ok(new StandardResponse<>("Cart fetched or created successfully", cartDTO));
        } catch (RuntimeException e) {
            logger.error("Error fetching or creating cart for user ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<StandardResponse<CartDTO>> getOrCreateGuestCart(@PathVariable String sessionId) {
        try {
            logger.info("Fetching or creating cart for session ID: {}", sessionId);
            CartDTO cartDTO = cartService.getOrCreateCart(sessionId, null); // userId is null for guest cart
            return ResponseEntity.ok(new StandardResponse<>("Cart fetched or created successfully", cartDTO));
        } catch (RuntimeException e) {
            logger.error("Error fetching or creating cart for session ID: {}", e.getMessage(), e);
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
            @RequestParam String guestSessionId,
            @RequestParam Long userId) {
        try {
            logger.info("Merging guest cart with session ID: {} into user cart for user ID: {}", guestSessionId, userId);
            CartDTO updatedCart = cartService.mergeCarts(guestSessionId, userId);
            return ResponseEntity.ok(new StandardResponse<>("Carts merged successfully", updatedCart));
        } catch (RuntimeException e) {
            logger.error("Error merging carts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }  catch (Exception e) {
            logger.error("Unexpected error while merging carts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("An unexpected error occurred", null));
        }
    }


}

