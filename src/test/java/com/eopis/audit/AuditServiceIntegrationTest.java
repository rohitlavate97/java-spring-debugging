package com.eopis.audit;

import com.eopis.audit.entity.AuditLog;
import com.eopis.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Test
    @DisplayName("Verify audit log recording and lookup by entity")
    void shouldRecordAndRetrieveAuditLog() {
        UUID userId = UUID.randomUUID();
        AuditLog logged = auditService.recordAudit(userId, "PRICE_UPDATE", "Product", "PROD-381", "corr-999");

        assertNotNull(logged);
        assertNotNull(logged.getId());

        List<AuditLog> logs = auditService.getAuditLogsForEntity("Product", "PROD-381");
        assertEquals(1, logs.size());
        assertEquals("PRICE_UPDATE", logs.get(0).getAction());
        assertEquals("corr-999", logs.get(0).getCorrelationId());
    }
}
