package com.eopis.product.service;

import com.eopis.product.dto.ProductResponseDto;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponseDto getProductBySku(String sku) {
        log.info("Fetching product from DATABASE for SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));
        return ProductResponseDto.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(ProductResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#sku")
    public ProductResponseDto updateProductPrice(String sku, BigDecimal newPrice) {
        log.info("Updating price in DATABASE and evicting cache for SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));
        product.setPrice(newPrice);
        Product saved = productRepository.save(product);
        return ProductResponseDto.fromEntity(saved);
    }
}
