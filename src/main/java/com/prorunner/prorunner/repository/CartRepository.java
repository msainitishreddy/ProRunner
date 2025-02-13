package com.prorunner.prorunner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.prorunner.prorunner.model.Cart;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>{


    @Query("SELECT c FROM Cart c JOIN c.cartProducts cp WHERE cp.product.id = :productId")
    List<Cart> findCartsByProductId(@Param("productId") Long productId);

    Optional<Cart> findById(Long cartId);

    Optional<Cart> findByUserId(Long userId); // For logged-in users

    Optional<Cart> findBySessionId(String sessionId); // For guest carts
}
