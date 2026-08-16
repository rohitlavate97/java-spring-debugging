package com.eopis.product;

import com.eopis.common.lock.DistributedLockService;
import com.eopis.product.dto.ProductResponseDto;
import com.eopis.product.entity.Category;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.CategoryRepository;
import com.eopis.product.repository.ProductRepository;
import com.eopis.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }

        Category electronics = categoryRepository.findByCode("CAT-ELEC-TEST")
                .orElseGet(() -> categoryRepository.save(new Category("Electronics", "CAT-ELEC-TEST", "Devices")));

        if (productRepository.findBySku("PROD-TEST-1").isEmpty()) {
            Product product = new Product("PROD-TEST-1", "Cache Test Product", "Description", new BigDecimal("199.99"), "USD");
            product.setCategory(electronics);
            productRepository.save(product);
        }
    }

    @Test
    @DisplayName("Verify product lookup populates cache and subsequent reads hit cache")
    void shouldCacheProductLookup() {
        // First lookup - populates cache
        ProductResponseDto first = productService.getProductBySku("PROD-TEST-1");
        assertNotNull(first);
        assertNotNull(cacheManager.getCache("products").get("PROD-TEST-1"));

        // Second lookup - returns from cache
        ProductResponseDto second = productService.getProductBySku("PROD-TEST-1");
        assertEquals(first.getPrice(), second.getPrice());

        // Price update - evicts cache
        productService.updateProductPrice("PROD-TEST-1", new BigDecimal("249.99"));
        assertNull(cacheManager.getCache("products").get("PROD-TEST-1"));
    }

    @Test
    @DisplayName("Verify distributed locking acquire, mutual exclusion, and release")
    void shouldAcquireAndReleaseDistributedLock() {
        String lockKey = "lock:inventory:wh17:prod381";
        
        // Acquire lock
        String lockToken = lockService.acquireLock(lockKey, Duration.ofSeconds(5));
        assertNotNull(lockToken);

        // Second acquire on same key fails
        String secondToken = lockService.acquireLock(lockKey, Duration.ofSeconds(5));
        assertNull(secondToken);

        // Release lock
        boolean released = lockService.releaseLock(lockKey, lockToken);
        assertTrue(released);

        // Now can acquire again
        String thirdToken = lockService.acquireLock(lockKey, Duration.ofSeconds(5));
        assertNotNull(thirdToken);
        lockService.releaseLock(lockKey, thirdToken);
    }
}
