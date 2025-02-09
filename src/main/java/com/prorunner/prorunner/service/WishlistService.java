package com.prorunner.prorunner.service;

import com.prorunner.prorunner.dto.WishlistDTO;
import com.prorunner.prorunner.dto.WishlistItemDTO;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.model.Wishlist;
import com.prorunner.prorunner.model.WishlistItem;
import com.prorunner.prorunner.repository.CartRepository;
import com.prorunner.prorunner.repository.ProductRepository;
import com.prorunner.prorunner.repository.UserRepository;
import com.prorunner.prorunner.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class WishlistService {        //wishlistService

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Helper method to map Wishlist to WishlistDTO
    private WishlistDTO mapToDTO(Wishlist wishlist) {
        WishlistDTO wishlistDTO = modelMapper.map(wishlist, WishlistDTO.class);
        wishlistDTO.setItems(wishlist.getItems().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
        return wishlistDTO;
    }

    // Helper method to map WishlistItem to WishlistItemDTO
    private WishlistItemDTO mapToDTO(WishlistItem item) {
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        return dto;
    }

    @Transactional
    public WishlistDTO getOrCreateWishlist(Long userId){
        return wishlistRepository.findById(userId)
                .map(this::mapToDTO)
                .orElseGet(()->{
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found with ID: "+userId));

                    Wishlist wishlist = new Wishlist();
                    wishlist.setUser(user);
                    wishlistRepository.save(wishlist);
                    return mapToDTO(wishlist);
                });
    }

    @Transactional
    public WishlistDTO addProductToWishlist(Long userId, Long productId){
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(user);
                    return wishlistRepository.save(newWishlist);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        // Ensure wishlist is persisted before adding items
        if (wishlist.getId() == null) {
            wishlist = wishlistRepository.save(wishlist);
        }

        // Check if the product is already in the wishlist
        boolean productExists = wishlist.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));

        if (!productExists) {
            WishlistItem item = new WishlistItem();
            item.setProduct(product);
            item.setWishlist(wishlist); // Ensure wishlist reference is set
            wishlist.getItems().add(item);
        }

        wishlistRepository.save(wishlist);
        return mapToDTO(wishlist);
    }


    @Transactional
    public WishlistDTO removeProductFromWishlist(Long userId, Long productId){
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found for user with ID: "+userId));

        boolean removed = wishlist.getItems().removeIf(item -> item.getProduct().getId().equals(productId));

        if (!removed) {
            throw new RuntimeException("Product not found in wishlist");
        }

        wishlistRepository.save(wishlist);

        return mapToDTO(wishlist);
    }

}
