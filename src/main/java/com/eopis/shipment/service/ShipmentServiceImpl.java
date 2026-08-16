package com.eopis.shipment.service;

import com.eopis.shipment.entity.Shipment;
import com.eopis.shipment.entity.ShipmentTracking;
import com.eopis.shipment.repository.ShipmentRepository;
import com.eopis.shipment.repository.ShipmentTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository, ShipmentTrackingRepository trackingRepository) {
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
    }

    @Override
    @Transactional
    public Shipment createShipment(Long orderId, String carrier, String trackingNumber) {
        log.info("Creating shipment for Order ID: {}, Carrier: {}", orderId, carrier);
        String shipmentNumber = "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Shipment shipment = new Shipment(shipmentNumber, orderId, carrier, trackingNumber);
        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public Shipment addTrackingEvent(Long shipmentId, String status, String location, String description) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found with ID: " + shipmentId));

        ShipmentTracking tracking = new ShipmentTracking(status, location, description);
        shipment.addTrackingEvent(tracking);
        shipment.setStatus(status);
        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentByNumber(String shipmentNumber) {
        return shipmentRepository.findByShipmentNumber(shipmentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found with number: " + shipmentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsForOrder(Long orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }
}
