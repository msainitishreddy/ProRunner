package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.service.CartService;
import com.prorunner.prorunner.util.StandardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import com.prorunner.prorunner.model.Product;
import com.prorunner.prorunner.service.ProductService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;



    @GetMapping
    public ResponseEntity<StandardResponse<Page<Product>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        try {
            Page<Product> products = productService.filterProducts(
                    category, gender, color, size, minPrice, maxPrice, availability, page, pageSize, sortBy
            );
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }



//    @PostMapping
//    public ResponseEntity<StandardResponse<Product>> addProduct(@RequestBody Product product) {
//        try {
//            Product savedProduct = productService.saveProduct(product);
//            return ResponseEntity.ok(new StandardResponse<>("Products added successfully", savedProduct));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new StandardResponse<>(e.getMessage(), null));
//        }
//    }

    // only for admins to add or update product information
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<Product>> addOrUpdateProduct(@RequestBody Product product) {
        try {
            Product savedProduct = productService.saveOrUpdateProduct(product);
            return ResponseEntity.ok(new StandardResponse<>("Product saved successfully", savedProduct));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    // Delete a product from the database
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<String>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(new StandardResponse<>("Product deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<Product>> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);

            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new StandardResponse<>("Product Not Found", null));

            }
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", product));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    public ResponseEntity<StandardResponse<String>> deleteProduct(@PathVariable Long id) {
//        try {
//             Product product = productService.getProductById(id);
//             if(product == null){
//                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new StandardResponse<>("Product Not Found", null));
//             }
//             cartService.removeProductFromAllCarts(id);
//             productService.deleteProduct(id);
//
//             return ResponseEntity.ok(new StandardResponse<>("Product deleted successfully", null));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new StandardResponse<>(e.getMessage(), null));
//        }
//    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<List<Product>>> addProducts(@RequestBody List<Product> products) {
        try {
            List<Product> savedProducts = productService.saveAllProducts(products);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new StandardResponse<>("Products added successfully", savedProducts));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getLocalizedMessage(), null));
        }
    }

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<Product>> updateProductStock(@PathVariable Long productId, @RequestParam int stock) {
        try {
            Product updatedProduct = productService.updateProductStock(productId, stock);
            return ResponseEntity.ok(new StandardResponse<>("Product stock updated successfully", updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new StandardResponse<>(e.getMessage(), null));
        }
    }


    @GetMapping("/paginated")
    public ResponseEntity<StandardResponse<Page<Product>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        try {
            Page<Product> products = productService.getProducts(page, size, sortBy);
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", products));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }




}