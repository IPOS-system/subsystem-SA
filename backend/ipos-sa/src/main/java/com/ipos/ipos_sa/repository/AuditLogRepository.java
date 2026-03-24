package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
