package com.eopis.inventory.repository;

import com.eopis.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByProductId(Long productId);

    Optional<Inventory> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.quantityAvailable >= :requiredQuantity")
    List<Inventory> findAvailableStock(@Param("productId") Long productId, @Param("requiredQuantity") int requiredQuantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdWithPessimisticLock(@Param("id") Long id);
}
