package com.allochub.audit;

import com.allochub.global.security.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void logCreate(AuthUser user, String entityType, String entityId, Object value) {
        AuditLog log = new AuditLog();
        log.setUserId(user.id());
        log.setAction("CREATE");
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setNewValue(toJson(value));
        repository.save(log);
    }

    public void logUpdate(
            AuthUser user, String entityType, String entityId, Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        log.setUserId(user.id());
        log.setAction("UPDATE");
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        repository.save(log);
    }

    public List<AuditLog> listRecent() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
