package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.payload.LoginRequest;
import com.prorunner.prorunner.repository.UserRepository;
import com.prorunner.prorunner.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public Map<String, String> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Extract roles
        var userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toSet());

        //Fetch User
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(()-> new RuntimeException("User not found"));

        // Generate token
        String token = jwtUtil.generateToken(user.getId(),userDetails.getUsername(), roles);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", token);
        return response;
    }
}
