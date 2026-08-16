package com.eopis.audit.service;

import com.eopis.audit.entity.AuditLog;
import java.util.List;
import java.util.UUID;

public interface AuditService {
    AuditLog recordAudit(UUID userId, String action, String entityType, String entityId, String correlationId);
    List<AuditLog> getAuditLogsForEntity(String entityType, String entityId);
}
