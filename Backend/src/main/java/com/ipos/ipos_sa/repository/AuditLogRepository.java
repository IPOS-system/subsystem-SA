package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

  /** Filter audit log by the user who performed the action. */
  List<AuditLog> findByUser_UserIdOrderByLoggedAtDesc(Integer userId);

  /** Filter by the type and ID of the affected record. */
  List<AuditLog> findByTargetTypeAndTargetIdOrderByLoggedAtDesc(String targetType, String targetId);

  /** Filter by date range. */
  List<AuditLog> findByLoggedAtBetweenOrderByLoggedAtDesc(LocalDateTime from, LocalDateTime to);
}
