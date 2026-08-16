package com.eopis.inventory.repository;

import com.eopis.inventory.entity.InventoryReservation;
import com.eopis.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByOrderId(Long orderId);
    List<InventoryReservation> findByStatus(ReservationStatus status);
}
