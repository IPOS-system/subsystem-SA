package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "monthly_discount_tracker",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_merchant_month",
            columnNames = {"merchant_id", "year_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyDiscountTracker {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tracker_id")
  private Integer trackerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id", nullable = false)
  private Merchant merchant;

  @Column(name = "year_month", nullable = false, length = 7)
  private String yearMonth;

  @Column(name = "total_order_value", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalOrderValue;

  @Column(name = "discount_rate", precision = 5, scale = 2)
  private BigDecimal discountRate;

  @Column(name = "discount_amount", precision = 10, scale = 2)
  private BigDecimal discountAmount;

  @Column(name = "settled", nullable = false)
  private Boolean settled;

  @Enumerated(EnumType.STRING)
  @Column(name = "settled_method")
  private SettledMethod settledMethod;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public enum SettledMethod {
    CHEQUE,
    DEDUCTED_FROM_ORDER
  }
}
