package com.eopis.shipment;

import com.eopis.shipment.entity.Shipment;
import com.eopis.shipment.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShipmentServiceIntegrationTest {

    @Autowired
    private ShipmentService shipmentService;

    @Test
    @DisplayName("Verify creating a shipment and appending tracking milestones")
    void shouldCreateShipmentAndAddTracking() {
        Shipment shipment = shipmentService.createShipment(1001L, "FEDEX", "TRK-987654321");

        assertNotNull(shipment);
        assertNotNull(shipment.getId());
        assertTrue(shipment.getShipmentNumber().startsWith("SHP-"));
        assertEquals("CREATED", shipment.getStatus());

        Shipment updated = shipmentService.addTrackingEvent(shipment.getId(), "IN_TRANSIT", "Chicago Hub", "Departed origin sorting facility");
        assertEquals("IN_TRANSIT", updated.getStatus());
        assertEquals(1, updated.getTrackingEvents().size());
    }
}
