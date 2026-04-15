package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "discount_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountTier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tier_id")
  private Integer tierId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private DiscountPlan discountPlan;

  @Column(name = "min_order_val", nullable = false, precision = 10, scale = 2)
  private BigDecimal minOrderVal;

  @Column(name = "max_order_val", precision = 10, scale = 2)
  private BigDecimal maxOrderVal;

  @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal discountRate;
}
