package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
}
