package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Invoice raised automatically when an Order is accepted (IPOS-SA-ORD).
 *
 * <p>Payment status transitions: PENDING → PARTIAL (partial payment received) PENDING → PAID (full
 * payment received) PARTIAL → PAID (remaining balance settled) PENDING → OVERDUE (due_date passed,
 * auto-flagged by scheduled job)
 *
 * <p>The merchant's account state (NORMAL / SUSPENDED / IN_DEFAULT) is driven by how many days past
 * due_date the invoice remains unpaid (see Merchant.AccountState).
 *
 * <p>amount_due tracks the outstanding balance; it starts at (gross_amount - discount_amount) and
 * is reduced as Payment records are recorded by the accounting department.
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

  @Id
  @Column(name = "invoice_id", length = 20)
  private String invoiceId;

  /** One invoice per order. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false, unique = true)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id", nullable = false)
  private Merchant merchant;

  @Column(name = "invoice_date", nullable = false)
  private java.time.LocalDateTime invoiceDate;

  /** Payment must be received by end of the calendar month (typically). */
  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  /** Remaining balance owed for this invoice. */
  @Column(name = "amount_due", nullable = false, precision = 10, scale = 2)
  private BigDecimal amountDue;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false)
  private PaymentStatus paymentStatus;

  @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
  private List<Payment> payments;

  public enum PaymentStatus {
    PENDING,
    PAID,
    OVERDUE
  }
}
