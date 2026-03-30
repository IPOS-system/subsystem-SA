package com.ipos.ipos_sa.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a payment made by a merchant against an Invoice (IPOS-SA-ORD).
 *
 * Payments are entered manually by the InfoPharma accounting department after receiving
 * the actual bank transfer, card payment, or cheque. Each payment reduces Invoice.amount_due
 * and, when fully settled, sets Invoice.payment_status to PAID.
 *
 * A single invoice may have multiple Payment records (partial payments).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    /** Bank / card / cheque reference number provided by the merchant. */
    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** The IPOS-SA user (accountant / admin) who recorded this payment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    public enum PaymentMethod {
        BANK_TRANSFER, CARD, CHEQUE
    }
}
