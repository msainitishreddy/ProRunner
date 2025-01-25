package com.prorunner.prorunner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.prorunner.prorunner.model.Product;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

}
