package com.allochub.controller;

import com.allochub.audit.AuditLog;
import com.allochub.audit.AuditService;
import com.allochub.global.response.ApiResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<Map<String, Object>>>> list() {
        List<Map<String, Object>> logs = auditService.listRecent().stream()
                .map(this::toMap)
                .toList();
        return ApiResponse.ok(Map.of("logs", logs));
    }

    private Map<String, Object> toMap(AuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getUserId());
        map.put("userId", log.getUserId());
        map.put("action", log.getAction());
        map.put("entityType", log.getEntityType());
        map.put("entityId", log.getEntityId());
        map.put("createdAt", log.getCreatedAt().toString());
        return map;
    }
}
