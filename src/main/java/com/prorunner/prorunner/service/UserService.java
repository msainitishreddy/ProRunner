package com.prorunner.prorunner.service;

import com.prorunner.prorunner.model.Address;
import com.prorunner.prorunner.model.User;
import com.prorunner.prorunner.model.Cart;
import com.prorunner.prorunner.repository.AddressRepository;
import com.prorunner.prorunner.repository.CartRepository;
import com.prorunner.prorunner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.prorunner.prorunner.exception.EmailAlreadyExistsException; // Custom exception
import com.prorunner.prorunner.exception.UsernameAlreadyExistsException;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;



import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final CartRepository cartRepository;

    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final AddressRepository addressRepository;

    // Constructor-based injection
    public UserService(UserRepository userRepository, CartRepository cartRepository, @Lazy PasswordEncoder passwordEncoder, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder; // Lazy injection to resolve circular dependency
        this.addressRepository = addressRepository;
    }

    public boolean doesUserExist(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<String> roles = user.getRoles();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );
    }

    public User registerUser(User user, Set<String> roles) {

        if(user.getPassword() == null || user.getPassword().trim().isEmpty()){
            throw new RuntimeException("Password cannot be null or empty!");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already in use!");
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (roles == null || roles.isEmpty()) {
            roles = Set.of("USER"); // Default role
        }

        user.setRoles(roles);

        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(user);

        user.setCart(cart);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Page<User> getUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return userRepository.findAll(pageable);
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public void deleteUserById(Long id){
        if(!userRepository.existsById(id)){
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
        logAdminAction("Admin deleted user with ID: " + id);
    }

    public User updateUser(User user){
        return userRepository.save(user);
    }

    public List<User> searchUsers(String username, String email, String role) {

        if((username == null || username.isEmpty()) &&
                (email == null || email.isEmpty()) &&
                (role == null || role.isEmpty())){
            return userRepository.findAll();
        }

        return userRepository.findAll().stream()
                .filter(user -> (username == null || user.getUsername().equalsIgnoreCase(username)) &&
                        (email == null || user.getEmail().equalsIgnoreCase(email)) &&
                        (role == null || user.getRoles().contains(role)))
                .collect(Collectors.toList());
    }

    private final List<String> adminLogs = new ArrayList<>();

    public void logAdminAction(String action) {
        adminLogs.add(action);
    }

    public List<String> getAdminLogs() {
        return new ArrayList<>(adminLogs);
    }


    public User updateUserRole(Long id, Set<String> roles) {
        User user = getUserById(id);
        user.setRoles(roles);
        return userRepository.save(user);
    }



    public Page<User> searchUsersWithPagination(String username, String email, String role, Pageable pageable) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("username"), "%" + username + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("email"), "%" + email + "%"));
            }
            if (role != null && !role.isEmpty()) {
                predicates.add(criteriaBuilder.isMember(role, root.get("roles")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable);
    }


    /// address
    public Address addAddress(Long userId, Address address) {
        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Associate user with address
        address.setUser(user);

        // Save address
        return addressRepository.save(address);
    }


    public Address updateAddress(Long userId, Long addressId, Address updatedAddress){

        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new RuntimeException("Address not found."));

        if(!address.getUser().getId().equals(userId)){
            throw new RuntimeException("Unauthorised access.");
        }

        address.setStreet(updatedAddress.getStreet());
        address.setCity(updatedAddress.getCity());
        address.setState(updatedAddress.getState());
        address.setCountry(updatedAddress.getCountry());
        address.setPostalCode(updatedAddress.getPostalCode());

        return addressRepository.save(address);
    }

    public void deleteAddress(Long userId, Long addressId){

        Address address = addressRepository.findById(addressId)
                .orElseThrow(()->new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)){
            throw new RuntimeException("Unauthorized access");
        }

        addressRepository.delete(address);
    }



}
