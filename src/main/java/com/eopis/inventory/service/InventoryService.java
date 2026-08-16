package com.eopis.inventory.service;

import com.eopis.inventory.entity.Inventory;
import com.eopis.inventory.entity.InventoryReservation;
import com.eopis.inventory.entity.ReservationStatus;
import com.eopis.inventory.repository.InventoryRepository;
import com.eopis.inventory.repository.InventoryReservationRepository;
import com.eopis.order.entity.Order;
import com.eopis.order.entity.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final int RESERVATION_EXPIRY_MINUTES = 30;

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public List<InventoryReservation> reserveStockForOrder(Order order) {
        log.info("Attempting to reserve inventory for Order #{}", order.getOrderNumber());
        List<InventoryReservation> reservations = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            int requiredQty = item.getQuantity();

            List<Inventory> stockRecords = inventoryRepository.findAvailableStock(productId, requiredQty);
            if (stockRecords.isEmpty()) {
                throw new IllegalStateException("Insufficient stock available across warehouses for product SKU: " 
                        + item.getProduct().getSku());
            }

            Inventory selectedInventory = stockRecords.get(0);
            selectedInventory.allocateStock(requiredQty);
            inventoryRepository.save(selectedInventory);

            InventoryReservation reservation = new InventoryReservation(
                    order,
                    selectedInventory,
                    requiredQty,
                    OffsetDateTime.now().plusMinutes(RESERVATION_EXPIRY_MINUTES)
            );
            reservations.add(reservationRepository.save(reservation));
        }

        log.info("Successfully reserved {} inventory lines for Order #{}", reservations.size(), order.getOrderNumber());
        return reservations;
    }

    @Transactional
    public void confirmReservation(Long orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.PENDING) {
                res.setStatus(ReservationStatus.CONFIRMED);
                Inventory inv = res.getInventory();
                inv.deductAllocatedStock(res.getQuantity());
                inventoryRepository.save(inv);
                reservationRepository.save(res);
            }
        }
        log.info("Confirmed reservations and deducted stock for Order ID: {}", orderId);
    }

    @Transactional
    public void releaseReservation(Long orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.PENDING || res.getStatus() == ReservationStatus.CONFIRMED) {
                res.setStatus(ReservationStatus.RELEASED);
                Inventory inv = res.getInventory();
                inv.releaseStock(res.getQuantity());
                inventoryRepository.save(inv);
                reservationRepository.save(res);
            }
        }
        log.info("Released reservations and returned stock for Order ID: {}", orderId);
    }
}
