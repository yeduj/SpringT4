package com.fresco.ecommerce.controllers;

import com.fresco.ecommerce.config.JwtUtil;
import com.fresco.ecommerce.models.Cart;
import com.fresco.ecommerce.models.CartProduct;
import com.fresco.ecommerce.models.Product;
import com.fresco.ecommerce.models.User;
import com.fresco.ecommerce.repo.CartProductRepo;
import com.fresco.ecommerce.repo.CartRepo;
import com.fresco.ecommerce.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth/consumer")
public class ConsumerController {

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CartProductRepo cartProductRepo;

    @Autowired
    private ProductRepo productRepo;

    @GetMapping("/cart")
    public ResponseEntity<Object> getCart(@RequestHeader(name = "JWT") String jwt) {
        String username = jwtUtil.getUser(jwt).getUsername();
        return ResponseEntity.ok(cartRepo.findByUserUsername(username).get());
    }

    @PostMapping("/cart")
    public ResponseEntity<Object> postCart(@RequestHeader(name = "JWT") String jwt, @RequestBody Product product) {
        User user = jwtUtil.getUser(jwt);
        Optional<Cart> cartOptional = cartRepo.findByUserUsername(user.getUsername());
        Cart cart;
        if (cartOptional.isPresent()) {
            cart = cartOptional.get();
        } else {
            cart = new Cart();
            cart.setUser(user);
            cartRepo.save(cart);
        }

        Optional<Product> productOptional = productRepo.findById(product.getProductId());
        Product existingProduct = productOptional.get();
        CartProduct cartProduct;
        Optional<CartProduct> cartProductOptional = cartProductRepo.findByCartUserUserIdAndProductProductId(user.getUserId(), existingProduct.getProductId());
        if (cartProductOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } else {
            cartProduct = new CartProduct(cart, existingProduct, 1);
        }
        cartProductRepo.save(cartProduct);
        cartRepo.save(cart);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/cart")
    public ResponseEntity<Object> putCart(@RequestHeader(name = "JWT") String jwt, @RequestBody CartProduct cartProduct) {
        User user = jwtUtil.getUser(jwt);
        Optional<Cart> optionalCart = cartRepo.findByUserUsername(user.getUsername());
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Optional<Product> optionalProduct = productRepo.findById(cartProduct.getProduct().getProductId());
        if (!optionalProduct.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Product existingProduct = optionalProduct.get();
        Optional<CartProduct> optionalExistingCartProduct = cartProductRepo.findByCartUserUserIdAndProductProductId(user.getUserId(),
                existingProduct.getProductId());
        if (cartProduct.getQuantity() == 0) {
            if (optionalExistingCartProduct.isPresent()) {
                CartProduct existingCartProduct = optionalExistingCartProduct.get();
                cart.updateTotalAmount(-existingCartProduct.getProduct().getPrice() * existingCartProduct.getQuantity());
                cartProductRepo.deleteByCartUserUserIdAndProductProductId(user.getUserId(),existingProduct.getProductId());
            }
        } else {
            if (optionalExistingCartProduct.isPresent()) {
                CartProduct existingCartProduct = optionalExistingCartProduct.get();
                cart.updateTotalAmount(-existingCartProduct.getProduct().getPrice() * existingCartProduct.getQuantity());
                existingCartProduct.setQuantity(cartProduct.getQuantity());
                cart.updateTotalAmount(existingProduct.getPrice() * existingCartProduct.getQuantity());
                cartProductRepo.save(existingCartProduct);
                //cartRepo.save(cart);
            } else {
                CartProduct newCartProduct = new CartProduct(cart, existingProduct, cartProduct.getQuantity());
                cartProductRepo.save(newCartProduct);
                cart.updateTotalAmount(existingProduct.getPrice() * newCartProduct.getQuantity());
                cartRepo.save(cart);
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Object> deleteCart(@RequestHeader(name = "JWT") String jwt, @RequestBody Product product) {
        // Get the authenticated user from the JWT token
        User user = jwtUtil.getUser(jwt);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT");
        }

        // Find the user's cart
        Optional<Cart> optionalCart = cartRepo.findByUserUsername(user.getUsername());
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart not found");
        }

        Optional<Product> optionalProduct = productRepo.findById(product.getProductId());
        if (!optionalProduct.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Product existingProduct = optionalProduct.get();

        // Find the existing CartProduct in the cart, if present
        Optional<CartProduct> optionalExistingCartProduct = cartProductRepo.findByCartUserUserIdAndProductProductId(
                user.getUserId(), existingProduct.getProductId());

        // If the CartProduct exists, delete it from the cart
        if (optionalExistingCartProduct.isPresent()) {
            CartProduct existingCartProduct = optionalExistingCartProduct.get();
            cart.updateTotalAmount(-existingCartProduct.getProduct().getPrice() * existingCartProduct.getQuantity());
            cartProductRepo.delete(existingCartProduct);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
