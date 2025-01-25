package com.prorunner.prorunner.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

    public boolean isUser(Long userId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // Logic to check if the current user matches the provided userId
        return true; // Replace with actual logic
    }

    public boolean isOrderOwner(Long orderId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // Logic to check if the current user owns the provided orderId
        return true; // Replace with actual logic
    }

    public boolean isCartOwner(Long cartId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // Verify if the logged-in user owns the provided cart
        return true; // Replace with actual logic
    }
}
