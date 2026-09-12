package com.context_mcp.context_mcp.application.suppression;

import com.context_mcp.context_mcp.application.audit.AuditService;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.SuppressionCommand;
import com.context_mcp.context_mcp.config.ContextPlatformProperties;
import com.context_mcp.context_mcp.domain.enums.AuditAction;
import com.context_mcp.context_mcp.domain.model.SuppressionRuleEntity;
import com.context_mcp.context_mcp.repository.SuppressionRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SuppressionService {

    private final SuppressionRuleRepository repository;
    private final AuditService auditService;
    private final ContextPlatformProperties properties;

    public SuppressionService(SuppressionRuleRepository repository,
                              AuditService auditService,
                              ContextPlatformProperties properties) {
        this.repository = repository;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public UUID suppress(SuppressionCommand command) {
        SuppressionRuleEntity rule = new SuppressionRuleEntity();
        rule.setConversationId(command.conversationId());
        rule.setCheckpointId(command.checkpointId());
        rule.setScope(command.scope().name());
        rule.setReason(command.reason());
        rule.setActive(true);

        if (command.expiresAt() != null) {
            rule.setExpiresAt(command.expiresAt());
        } else {
            rule.setExpiresAt(Instant.now().plus(properties.getSecurity().getSuppressionDefaultDuration()));
        }

        rule = repository.save(rule);

        auditService.record(AuditAction.SUPPRESSION_CREATED, "SUPPRESSION_RULE", rule.getId(),
                Map.of("scope", command.scope().name(),
                       "conversationId", command.conversationId() != null ? command.conversationId() : ""));
        return rule.getId();
    }

    public boolean isActiveSuppression(String conversationId, UUID checkpointId) {
        return !repository.findActiveSuppression(
                conversationId != null ? conversationId : "",
                checkpointId,
                Instant.now()).isEmpty();
    }
}
