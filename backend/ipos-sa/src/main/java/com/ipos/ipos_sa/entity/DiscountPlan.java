package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Represents a discount plan that can be assigned to a merchant. Supports FIXED (single rate) and
 * FLEXIBLE (tiered by monthly spend) plan types.
 */
@Entity
@Table(name = "discount_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountPlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "plan_id")
  private Integer planId;

  @Column(name = "plan_name", nullable = false, length = 100)
  private String planName;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_type", nullable = false)
  private PlanType planType;

  /** Used when plan_type = FIXED. Single discount rate applied to every order. */
  @Column(name = "fixed_rate", precision = 5, scale = 2)
  private BigDecimal fixedRate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "discountPlan", fetch = FetchType.LAZY)
  private List<DiscountTier> discountTiers;

  @OneToMany(mappedBy = "discountPlan", fetch = FetchType.LAZY)
  private List<Merchant> merchants;

  public enum PlanType {
    FIXED,
    FLEXIBLE
  }
}
