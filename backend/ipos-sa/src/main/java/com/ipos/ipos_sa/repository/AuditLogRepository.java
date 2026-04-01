package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    /** Filter audit log by the user who performed the action. */
    List<AuditLog> findByUser_UserIdOrderByLoggedAtDesc(Integer userId);

    /** Filter by the type and ID of the affected record (e.g. all changes to merchant #5). */
    List<AuditLog> findByTargetTypeAndTargetIdOrderByLoggedAtDesc(String targetType, String targetId);

    /** Filter by date range — used by the audit log screen with date pickers. */
    List<AuditLog> findByLoggedAtBetweenOrderByLoggedAtDesc(LocalDateTime from, LocalDateTime to);
}