package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem,Long> {
}
