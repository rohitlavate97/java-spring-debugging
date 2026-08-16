package com.eopis.inventory.entity;

import com.eopis.product.entity.Product;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uq_warehouse_product", columnNames = {"warehouse_id", "product_id"})
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "quantity_allocated", nullable = false)
    private int quantityAllocated;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Inventory() {
    }

    public Inventory(Warehouse warehouse, Product product, int quantityAvailable, int quantityAllocated) {
        this.warehouse = warehouse;
        this.product = product;
        this.quantityAvailable = quantityAvailable;
        this.quantityAllocated = quantityAllocated;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void allocateStock(int quantity) {
        if (this.quantityAvailable < quantity) {
            throw new IllegalStateException("Insufficient available stock for product: " + product.getSku());
        }
        this.quantityAvailable -= quantity;
        this.quantityAllocated += quantity;
    }

    public void releaseStock(int quantity) {
        this.quantityAllocated = Math.max(0, this.quantityAllocated - quantity);
        this.quantityAvailable += quantity;
    }

    public void deductAllocatedStock(int quantity) {
        if (this.quantityAllocated < quantity) {
            throw new IllegalStateException("Cannot deduct more than allocated stock");
        }
        this.quantityAllocated -= quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
