package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist,Long> {

    Optional<Wishlist> findByUserId(Long userId);
}
