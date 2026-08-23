package com.allochub.audit;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(
            String entityType, Collection<String> entityIds);
}
