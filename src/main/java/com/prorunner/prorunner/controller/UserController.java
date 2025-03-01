package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.UserRegistrationDTO;
import com.prorunner.prorunner.exception.EmailAlreadyExistsException;
import com.prorunner.prorunner.exception.UsernameAlreadyExistsException;
import com.prorunner.prorunner.model.Address;
import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.payload.LoginRequest;
import com.prorunner.prorunner.repository.UserRepository;
import com.prorunner.prorunner.service.UserService;
import com.prorunner.prorunner.util.JwtUtil;
import com.prorunner.prorunner.util.StandardResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            //fetching user details...
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(()->new RuntimeException("User not Found"));

            // Extract roles as a Set<String>
            Set<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .collect(Collectors.toSet());

            // Generate the token
            String token = jwtUtil.generateToken(user.getId(),userDetails.getUsername(), roles);

            return ResponseEntity.ok("Bearer " + token);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO userDTO) {
        try {
            // Map DTO to User entity
            User user = new User();
            user.setName(userDTO.getName());
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setPhoneNumber(userDTO.getPhoneNumber());
            user.setPassword(userDTO.getPassword());

            if (userDTO.getRoles() == null || userDTO.getRoles().isEmpty()) {
                user.setRoles(Set.of("USER"));
            } else {
                user.setRoles(userDTO.getRoles());
            }

            User registeredUser = userService.registerUser(user,userDTO.getRoles());
            return ResponseEntity.ok("User registered successfully with ID: " + registeredUser.getId());
        } catch (EmailAlreadyExistsException | UsernameAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id){
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email){
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()){
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with email: "+email);
    }

    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        try {
            Page<User> users = userService.getUsers(page, size, sortBy);
            if (users.hasContent()){
                return ResponseEntity.ok(users);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No Users Found");
            }
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/protected-endpoint")
    public ResponseEntity<String> getProtectedResource(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized: No token provided");
        }

        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            Set<String> roles = jwtUtil.extractRoles(token);

            return ResponseEntity.ok("Hello, " + username + "! Your roles are: " + roles);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid token");
        }
    }


    // Add address
    @PostMapping("/{userId}/addresses")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<Address>> addAddress(
            @PathVariable Long userId,
            @Valid @RequestBody Address address) {
        try {
            // Ensure user exists and associate the address
            Address savedAddress = userService.addAddress(userId, address);
            return ResponseEntity.ok(new StandardResponse<>("Address added successfully", savedAddress));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    @PutMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<Address>> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @RequestBody Address updatedAddress) {
        Address address = userService.updateAddress(userId, addressId, updatedAddress);
        return ResponseEntity.ok(new StandardResponse<>("Address updated successfully", address));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('USER') or @securityService.isUser(#userId)")
    public ResponseEntity<StandardResponse<String>> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(new StandardResponse<>("Address deleted successfully", null));
    }


}
