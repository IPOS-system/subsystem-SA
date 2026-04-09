package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Immutable audit trail of significant actions performed within IPOS-SA.
 *
 * <p>Every create / update / delete on sensitive entities (merchant accounts, orders, payments)
 * should produce an AuditLog entry. Records must never be updated or deleted.
 *
 * <p>entity_name + entity_id together identify the affected business object, e.g. entity_name =
 * "merchant", entity_id = 3 → Merchant #3.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id")
  private Integer logId;

  /** The user who performed the action. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * High-level description of the action, e.g. "CREATE_ACCOUNT", "UPDATE_CREDIT_LIMIT",
   * "PLACE_ORDER", "RECORD_PAYMENT".
   */
  @Column(name = "action", nullable = false, length = 100)
  private String action;

  /** Name of the affected database entity / table (e.g. "merchant", "orders"). */
  @Column(name = "target_type", length = 50)
  private String targetType;

  /** Primary key of the affected row. */
  @Column(name = "target_id", length = 50)
  private String targetId;

  @CreationTimestamp
  @Column(name = "logged_at", nullable = false, updatable = false)
  private LocalDateTime loggedAt;

  /** Human-readable description providing additional context for the action. */
  @Lob
  @Column(name = "details", columnDefinition = "TEXT")
  private String details;
}
