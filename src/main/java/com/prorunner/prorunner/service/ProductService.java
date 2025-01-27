package com.prorunner.prorunner.service;

import com.prorunner.prorunner.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    private ProductDTO mapToDTO(Product product) {
        return modelMapper.map(product, ProductDTO.class);
    }

    private Product mapToEntity(ProductDTO productDTO) {
        return modelMapper.map(productDTO, Product.class);
    }

    public List<ProductDTO> getAllProducts(){
        return productRepository.findAll().stream()
                .map(this::mapToDTO).toList();
    }

    // To save a single product
    public ProductDTO saveProduct(ProductDTO productDTO){
        Product product = mapToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return mapToDTO(product);
    }

    // Fetch product by its id
    public ProductDTO getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    // Delete a product by productId.
    public void deleteProduct(Long id){
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // To save a list of products at once
    public List<ProductDTO> saveAllProducts(List<ProductDTO> productDTOs) {
        List<Product> products = productDTOs.stream()
                .map(this::mapToEntity)
                .toList();
        List<Product> savedProducts = productRepository.saveAll(products);
        return savedProducts.stream()
                .map(this::mapToDTO)
                .toList();

    }

    // Update product stock
    public ProductDTO updateProductStock(Long productId, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " does not exist."));

        product.setStock(stock);
        Product updatedProduct = productRepository.save(product);
        return mapToDTO(updatedProduct);
    }

    // Update product details
    public ProductDTO updateProduct(Long productId, ProductDTO updatedProductDTO) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        existingProduct.setName(updatedProductDTO.getName());
        existingProduct.setDescription(updatedProductDTO.getDescription());
        existingProduct.setPrice(updatedProductDTO.getPrice());
        existingProduct.setSize(updatedProductDTO.getSize());
        existingProduct.setCategory(updatedProductDTO.getCategory());
        existingProduct.setImageUrl(updatedProductDTO.getImageUrl());
        existingProduct.setStock(updatedProductDTO.getStock());
        existingProduct.setAvailability(updatedProductDTO.getAvailability());
        existingProduct.setGender(updatedProductDTO.getGender());
        existingProduct.setColor(updatedProductDTO.getColor());

        Product updatedProduct = productRepository.save(existingProduct);
        return mapToDTO(updatedProduct);
    }

    // Fetch products with pagination
    public Page<ProductDTO> getProducts(int page, int size, String sortBy){
        Pageable pageable = PageRequest.of(page,size,Sort.by(sortBy));
        return productRepository.findAll(pageable)
                .map(this::mapToDTO);
    }


    // Filter products dynamically with pagination
    public Page<ProductDTO> filterProducts(
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
            var predicates = new ArrayList<Predicate>();

            if (category != null) predicates.add(criteriaBuilder.equal(root.get("category"), category));
            if (gender != null) predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            if (color != null) predicates.add(criteriaBuilder.equal(root.get("color"), color));
            if (size != null) predicates.add(criteriaBuilder.equal(root.get("size"), size));
            if (minPrice != null) predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (availability != null) predicates.add(criteriaBuilder.equal(root.get("availability"), availability));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(this::mapToDTO);
    }


    // Save or update a product
    public ProductDTO saveOrUpdateProduct(ProductDTO productDTO) {
        Product product = mapToEntity(productDTO);
        product = productRepository.save(product);
        return mapToDTO(product);
    }


}
