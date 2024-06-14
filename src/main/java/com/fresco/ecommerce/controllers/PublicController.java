package com.fresco.ecommerce.controllers;

import com.fresco.ecommerce.config.JwtUtil;
import com.fresco.ecommerce.models.Product;
import com.fresco.ecommerce.models.User;
import com.fresco.ecommerce.repo.ProductRepo;
import com.fresco.ecommerce.service.UserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/product/search")
    public List<Product> getProducts(@RequestParam String keyword) {
        return productRepo.findByProductNameContainingIgnoreCaseOrCategoryCategoryNameContainingIgnoreCase(keyword, keyword);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User requestBody) {
        User user = userAuthService.loadUserByUsername(requestBody.getUsername());
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestBody.getUsername()
                , requestBody.getPassword(), user.getAuthorities()));
        return ResponseEntity.ok(jwtUtil.generateToken(user));
    }
}