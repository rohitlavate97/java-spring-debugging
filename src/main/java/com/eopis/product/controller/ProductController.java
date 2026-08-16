package com.eopis.product.controller;

import com.eopis.product.dto.ProductResponseDto;
import com.eopis.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDto>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @PutMapping("/{sku}/price")
    public ResponseEntity<ProductResponseDto> updateProductPrice(@PathVariable String sku, @RequestParam BigDecimal newPrice) {
        return ResponseEntity.ok(productService.updateProductPrice(sku, newPrice));
    }
}
