package com.context_mcp.context_mcp.application.audit;

import com.context_mcp.context_mcp.domain.enums.AuditAction;
import com.context_mcp.context_mcp.domain.model.AuditEventEntity;
import com.context_mcp.context_mcp.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit log.  Raw memory content, secrets, and full artifact
 * text must never be passed in the details map.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String entityType, UUID entityId, Map<String, Object> details) {
        AuditEventEntity event = new AuditEventEntity();
        event.setAction(action.name());
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setActor("system");
        try {
            event.setDetails(objectMapper.writeValueAsString(details != null ? details : Map.of()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details for {}: {}", action, e.getMessage());
            event.setDetails("{}");
        }
        repository.save(event);
    }

    public void record(AuditAction action, String entityType, UUID entityId) {
        record(action, entityType, entityId, Map.of());
    }
}
