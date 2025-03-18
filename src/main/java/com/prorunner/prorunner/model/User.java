package com.prorunner.prorunner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch=FetchType.EAGER )
    @JsonManagedReference
    private List<Address> addresses = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Pattern(regexp = "\\d{10}", message = "Invalid phone number")
    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @JsonIgnore
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();


    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore // Avoid recursive serialization
    private List<Order> orders;


//    public @Pattern(regexp = "\\d{10}", message = "Invalid phone number") String getPhoneNumber() {
//        return phoneNumber;
//    }
//
//    public void setPhoneNumber(@Pattern(regexp = "\\d{10}", message = "Invalid phone number") String phoneNumber) {
//        this.phoneNumber = phoneNumber;
//    }

    public void setCart(Cart cart) {
        this.cart = cart;
        if (cart != null){
            cart.setUser(this);
        }
    }

}
