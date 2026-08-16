package com.eopis.product.service;

import com.eopis.product.dto.ProductResponseDto;
import java.util.List;

public interface ProductService {
    ProductResponseDto getProductBySku(String sku);
    List<ProductResponseDto> getProductsByCategory(Long categoryId);
    ProductResponseDto updateProductPrice(String sku, java.math.BigDecimal newPrice);
}
