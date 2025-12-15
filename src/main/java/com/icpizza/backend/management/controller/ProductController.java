package com.icpizza.backend.management.controller;

import com.icpizza.backend.management.dto.ProductTO;
import com.icpizza.backend.management.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;

    @GetMapping("/fetch_products")
    public ResponseEntity<List<ProductTO>> fetchProducts(){
        log.info("[REPORTS] fetching products");
        List<ProductTO> products = productService.fetchProducts();
        return ResponseEntity.ok(products);
    }
}
