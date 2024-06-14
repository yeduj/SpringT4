package com.fresco.ecommerce.controllers;

import com.fresco.ecommerce.config.JwtUtil;
import com.fresco.ecommerce.models.Category;
import com.fresco.ecommerce.models.Product;
import com.fresco.ecommerce.repo.ProductRepo;
import com.fresco.ecommerce.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/seller")
@Transactional
public class SellerController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepo userRepo;

    @PostMapping("/product")
    @Transactional
    public ResponseEntity<Object> postProduct(@RequestHeader(name = "JWT") String jwt, @RequestBody Product product) {
        product.setSeller(jwtUtil.getUser(jwt));
        Product response = productRepo.saveAndFlush(product);
        return ResponseEntity.status(HttpStatus.CREATED).body("http://localhost/api/auth/seller/product/" + response.getProductId());
    }

    @GetMapping("/product")
    public ResponseEntity<Object> getAllProducts(@RequestHeader(name = "JWT") String jwt) {
        return ResponseEntity.ok(productRepo.findBySellerUserId(jwtUtil.getUser(jwt).getUserId()));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Object> getProduct(@RequestHeader(name = "JWT") String jwt, @PathVariable Integer productId) {
        Optional<Product> product = productRepo.findById(productId);
        List<Product> products = productRepo.findBySellerUserId(jwtUtil.getUser(jwt).getUserId());
        if (product.isPresent() && products.contains(product.get())) {
            return new ResponseEntity<>(product.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/product")
    public ResponseEntity<Object> putProduct(@RequestHeader(name = "JWT") String jwt, @RequestBody Product product) {
        Optional<Product> prod = productRepo.findById(product.getProductId());
        if (prod.isPresent()) {
            prod.get().setProductName(product.getProductName());
            prod.get().setPrice(product.getPrice());
            Category category = new Category();
            category.setCategoryId(product.getCategory().getCategoryId());
            category.setCategoryName(product.getCategory().getCategoryName());
            prod.get().setCategory(category);
            productRepo.saveAndFlush(prod.get());
            return new ResponseEntity<>(prod.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Product> deleteProduct(@RequestHeader(name = "JWT") String jwt, @PathVariable Integer productId) {
        Optional<Product> optionalProduct = productRepo.findBySellerUserIdAndProductId(jwtUtil.getUser(jwt).getUserId(), productId);
        if (!optionalProduct.isPresent()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Product product = optionalProduct.get();
        product.setSeller(null);
        productRepo.delete(product);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }
}