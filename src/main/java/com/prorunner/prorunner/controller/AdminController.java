package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.UserRegistrationDTO;
import com.prorunner.prorunner.exception.EmailAlreadyExistsException;
import com.prorunner.prorunner.exception.UserNotFoundException;
import com.prorunner.prorunner.exception.UsernameAlreadyExistsException;
import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.service.SecurityService;
import com.prorunner.prorunner.service.UserService;
import com.prorunner.prorunner.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    private final List<String> adminLogs = new ArrayList<>();

    @PostMapping("/register")
    //@PreAuthorize("hasAuthority('ADMIN')")// remove this and try to test with exist security
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody UserRegistrationDTO userDTO) {
        try {
            // Map DTO to User entity
            User user = new User();
            user.setName(userDTO.getName());
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setPassword(userDTO.getPassword());
            user.setPhoneNumber(userDTO.getPhoneNumber());


            // Roles must include ADMIN for this endpoint
            if (userDTO.getRoles() == null || userDTO.getRoles().isEmpty()) {
                userDTO.getRoles().add("ADMIN");
            } else if (!userDTO.getRoles().contains("ADMIN")) {
                return ResponseEntity.badRequest().body("Admin role is required for this endpoint");
            }

            User registeredUser = userService.registerUser(user, userDTO.getRoles());
            return ResponseEntity.ok("Admin registered successfully with ID: " + registeredUser.getId());
        } catch (EmailAlreadyExistsException | UsernameAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    // Fetching all users - access for admin only
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Fetch all users", description = "Fetch all users with pagination and sorting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users fetched successfully"),
            @ApiResponse(responseCode = "404", description = "No users found")
    })
    public ResponseEntity<StandardResponse<List<User>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Page<User> users = userService.getUsers(page, size, sortBy);

        if (users.hasContent()) {
            users.getContent().forEach(user -> user.getRoles().size()); // Force roles initialization
            return ResponseEntity.ok(new StandardResponse<>("Users fetched successfully", users.getContent()));
        } else {
            throw new UserNotFoundException("No users found");
        }
    }

    // Fetch a specific user by ID
    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @securityService.isUser(#id)")
    @Operation(summary = "Fetch user by ID", description = "Fetch a user by their ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User fetched successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<StandardResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new StandardResponse<>("User fetched successfully", user));
    }

    // Delete a user by ID
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete user by ID", description = "Delete a user by their ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<StandardResponse<String>> deleteUser(@PathVariable Long userId) {
        if (!userService.doesUserExist(userId)) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }
        userService.deleteUserById(userId);
        return ResponseEntity.ok(new StandardResponse<>("User deleted successfully", null));
    }

    // update the user

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update user details", description = "Allows admin to update user details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<StandardResponse<?>> updateUser(
            @PathVariable Long id,
            @RequestBody User updatedUser){
        User user = userService.getUserById(id);
        user.setUsername(updatedUser.getUsername() != null ? updatedUser.getUsername() : user.getUsername());
        user.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : user.getEmail());
        user.setRoles(updatedUser.getRoles() != null ? updatedUser.getRoles() : user.getRoles());

        User savedUser = userService.updateUser(user);

        return ResponseEntity.ok(new StandardResponse<>("User updated successfully ", savedUser));
    }

    // update user role and access level - ADMIN

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Set<String> roles) {
        User updatedUser = userService.updateUserRole(id, roles);
        return ResponseEntity.ok(updatedUser);
    }

    // Search users by username, email, or roles

    @GetMapping("/users/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Search users", description = "Search users by username, email, or roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No users found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<StandardResponse<?>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<User> users = userService.searchUsersWithPagination(username, email, role, pageable);

        if (!users.hasContent()) {
            throw new UserNotFoundException("No users found matching the criteria");
        }

        return ResponseEntity.ok(
                new StandardResponse<>("Users fetched successfully", users.getContent())
        );
    }



    @PostMapping("/logs")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "View admin logs", description = "Fetch logs of admin actions")
    public ResponseEntity<StandardResponse<?>> getAdminLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page,size);
        List<String> logs = userService.getAdminLogs();
        int start = Math.min((int) pageable.getOffset(), logs.size());
        int end = Math.min((start + pageable.getPageSize()), logs.size());
        List<String> paginatedLogs = logs.subList(start, end);

        return ResponseEntity.ok(new StandardResponse<>("Logs fetched successfully", paginatedLogs));
    }



}

// getAdminLogs
// searchUsers
// updateUserRole
// updateUser
// deleteUser
// getUserById
// getAllUsers