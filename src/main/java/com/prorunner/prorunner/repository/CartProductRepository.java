package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.Cart;
import com.prorunner.prorunner.model.CartProduct;
import com.prorunner.prorunner.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartProductRepository extends JpaRepository<CartProduct , Long> {

    Optional<CartProduct> findByCartAndProduct(Cart cart, Product product);

    List<CartProduct> findByCart(Cart cart);

    void deleteAllByCart(Cart cart);

    long countByCart(Cart cart);

    List<CartProduct> findByProductId(Long productId);

    @Query("SELECT cp FROM CartProduct cp WHERE cp.cart = :cart")
    Page<CartProduct> findByCart(@Param("cart") Cart cart, Pageable pageable);

}

