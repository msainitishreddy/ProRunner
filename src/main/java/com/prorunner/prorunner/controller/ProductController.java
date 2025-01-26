package com.prorunner.prorunner.controller;

import com.prorunner.prorunner.dto.ProductDTO;
import com.prorunner.prorunner.service.CartService;
import com.prorunner.prorunner.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);


    /**
     * Fetch all products with optional filters and pagination.
     */

    @Operation(summary = "Fetch all products with optional filters and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<StandardResponse<Page<ProductDTO>>> getAllProducts(
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
            logger.info("Fetching products with filters and pagination");
            Page<ProductDTO> products = productService.filterProducts(
                    category, gender, color, size, minPrice, maxPrice, availability, page, pageSize, sortBy);
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", products));
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Error fetching products", null));
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

    /**
     * Add or update a product.
     * only for admins to add or update product information
     */

    @Operation(summary = "Add or update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product saved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<ProductDTO>> addOrUpdateProduct(@RequestBody ProductDTO productDTO) {
        try {
            logger.info("Adding/updating product: {}", productDTO.getName());
            ProductDTO savedProduct = productService.saveOrUpdateProduct(productDTO);
            return ResponseEntity.ok(new StandardResponse<>("Product saved successfully", savedProduct));
        } catch (Exception e) {
            logger.error("Error adding/updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Error saving product", null));
        }
    }

    /**
     * Add products in bulk.
     */

    @Operation(summary = "Add products in bulk")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Products added successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<List<ProductDTO>>> addProducts(@RequestBody List<ProductDTO> products) {
        try {
            logger.info("Adding products in bulk");
            List<ProductDTO> savedProducts = productService.saveAllProducts(products);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new StandardResponse<>("Products added successfully", savedProducts));
        } catch (Exception e) {
            logger.error("Error adding products in bulk: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Error adding products", null));
        }
    }


    /**
     * Update stock for a specific product.
     */
    @Operation(summary = "Update product stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid stock value"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<ProductDTO>> updateProductStock(@PathVariable Long productId, @RequestParam int stock) {
        try {
            logger.info("Updating stock for product ID: {}, new stock: {}", productId, stock);
            ProductDTO updatedProduct = productService.updateProductStock(productId, stock);
            return ResponseEntity.ok(new StandardResponse<>("Product stock updated successfully", updatedProduct));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid stock value: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new StandardResponse<>(e.getMessage(), null));
        } catch (RuntimeException e) {
            logger.error("Error updating product stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }
    }

    /**
     * Delete a product by ID.
     */
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse<String>> deleteProduct(@PathVariable Long id) {
        try {
            logger.info("Deleting product with ID: {}", id);

            //checking if product exists
            ProductDTO product = productService.getProductById(id);
            if (product == null) {
                logger.error("Product with ID {} not found", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new StandardResponse<>("Product not found with ID: " + id, null));
            }

            // Remove product from all carts
            logger.info("Removing product with ID {} from all carts", id);
            cartService.removeProductFromAllCarts(id);

            // Delete the product from the database
            productService.deleteProduct(id);
            logger.info("Product with ID {} deleted successfully", id);
            return ResponseEntity.ok(new StandardResponse<>("Product deleted successfully", null));
        } catch (RuntimeException e) {
            logger.error("Error deleting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StandardResponse<>(e.getMessage(), null));
        }catch (Exception e) {
            logger.error("Unexpected error deleting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Unexpected error occurred", null));
        }
    }

    /**
     * Fetch a product by ID.
     */

    @Operation(summary = "Fetch product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        try {
            logger.info("Fetching product with ID: {}", id);
            ProductDTO productDTO = productService.getProductById(id);
            return ResponseEntity.ok(new StandardResponse<>("Product fetched successfully", productDTO));
        } catch (Exception e) {
            logger.error("Error fetching product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
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



    /**
     * Fetch products with pagination.
     */

    @Operation(summary = "Fetch paginated products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/paginated")
    public ResponseEntity<StandardResponse<Page<ProductDTO>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        try {
            logger.info("Fetching paginated products");
            Page<ProductDTO> products = productService.getProducts(page, size, sortBy);
            return ResponseEntity.ok(new StandardResponse<>("Products fetched successfully", products));
        } catch (Exception e) {
            logger.error("Error fetching paginated products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StandardResponse<>("Error fetching products", null));
        }
    }


}