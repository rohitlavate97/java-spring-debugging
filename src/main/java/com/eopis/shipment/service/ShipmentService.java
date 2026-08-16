package com.eopis.shipment.service;

import com.eopis.shipment.entity.Shipment;
import com.eopis.shipment.entity.ShipmentTracking;
import java.util.List;

public interface ShipmentService {
    Shipment createShipment(Long orderId, String carrier, String trackingNumber);
    Shipment addTrackingEvent(Long shipmentId, String status, String location, String description);
    Shipment getShipmentByNumber(String shipmentNumber);
    List<Shipment> getShipmentsForOrder(Long orderId);
}
