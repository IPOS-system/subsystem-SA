package com.ipos.ipos_sa.dto.audit;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a single audit log entry. Returned by GET /api/audit-log with optional filter
 * params.
 */
@Data
@Builder
public class AuditLogDTO {

  private Integer logId;
  private Integer userId;
  private String username;

  /** High-level action description, e.g. "CREATE_ACCOUNT", "PLACE_ORDER". */
  private String action;

  /** The type of entity affected, e.g. "merchant", "order". */
  private String targetType;

  /** Primary key of the affected record. */
  private String targetId;

  /** Human-readable detail providing additional context. */
  private String details;

  private LocalDateTime loggedAt;
}
