package com.prorunner.prorunner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    public Product saveProduct(Product product){
        if (product.getStock() == null){
            product.setStock(10);
        }
        //product.updateStatus();
        return productRepository.save(product);
    }

    public Product getProductById(Long id){
        return productRepository.findById(id).orElse(null);
    }

    public void deleteProduct(Long id){
        productRepository.deleteById(id);
    }

    public List<Product> saveAllProducts(List<Product> products) {
        return productRepository.saveAll(products);
    }

    public Product updateProductStock(Long productId, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist."));

        product.setStock(stock);
        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, Product updatedProduct) {
        Product existingProduct = getProductById(productId);
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setSize(updatedProduct.getSize());
        existingProduct.setCategory(updatedProduct.getCategory());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());
        existingProduct.setStock(updatedProduct.getStock());
        //existingProduct.updateStatus();
        return productRepository.save(existingProduct);
    }

    public Page<Product> getProducts(int page, int size, String sortBy){
        Pageable pageable = PageRequest.of(page,size,Sort.by(sortBy));
        return productRepository.findAll(pageable);
    }

    public Page<Product> filterProducts(
            String category,
            String gender,
            String color,
            String size,
            Double minPrice,
            Double maxPrice,
            Boolean availability,
            int page,
            int pageSize,
            String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortBy));
        return productRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply filters dynamically
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (gender != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }
            if (color != null) {
                predicates.add(criteriaBuilder.equal(root.get("color"), color));
            }
            if (size != null) {
                predicates.add(criteriaBuilder.equal(root.get("size"), size));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (availability != null) {
                predicates.add(criteriaBuilder.equal(root.get("availability"), availability));
            }

            // Combine all predicates
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }


    public Product saveOrUpdateProduct(Product product) {
        if (product.getStock() == null || product.getStock() <= 0) {
            product.setAvailability(false); // Mark as out of stock
        } else {
            product.setAvailability(true);
        }
        return productRepository.save(product);
    }



}
