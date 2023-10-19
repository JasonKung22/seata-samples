package io.seata.product.controller;

import io.seata.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @PutMapping("/minus/stock")
    public ResponseEntity<Void> minusStock() {
        productService.minusStock();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/seataToSharding")
    public ResponseEntity<Void> seataToSharding(Boolean hasError) {
        productService.seataToSharding(hasError);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
