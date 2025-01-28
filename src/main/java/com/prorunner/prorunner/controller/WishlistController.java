package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.WishlistDTO;
import com.prorunner.prorunner.service.WishlistService;
import com.prorunner.prorunner.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private WishlistService wishlistService;

    @Operation(summary = "Get or create a wishlist", description = "Fetches the wishlist for a user or creates a new one if it doesn't exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wishlist fetched successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<WishlistDTO>> getOrCreateWishlist(@PathVariable Long userId){
        WishlistDTO wishlist = wishlistService.getOrCreateWishlist(userId);
        return ResponseEntity.ok(new StandardResponse<>("Wishlist fetched successfully", wishlist));
    }


    @Operation(summary = "Add a product to the wishlist", description = "Adds a product to the user's wishlist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added successfully"),
            @ApiResponse(responseCode = "404", description = "Product or user not found")
    })
    @PostMapping("/{userId}/add/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public  ResponseEntity<StandardResponse<WishlistDTO>> addProductToWishlist(@PathVariable Long userId, @PathVariable Long productId){
        WishlistDTO wishlist = wishlistService.addProductToWishlist(userId,productId);
        return ResponseEntity.ok(new StandardResponse<>("Product added to wishlist successfully", wishlist));
    }


    @Operation(summary = "Remove a product from the wishlist", description = "Removes a product from the user's wishlist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist or product not found")
    })
    @DeleteMapping("/{userId}/remove/{productId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<WishlistDTO>> removeProductFromWishlist(@PathVariable Long userId, @PathVariable Long productId) {
        WishlistDTO wishlist = wishlistService.removeProductFromWishlist(userId, productId);
        return ResponseEntity.ok(new StandardResponse<>("Product removed from wishlist successfully", wishlist));
    }

}
