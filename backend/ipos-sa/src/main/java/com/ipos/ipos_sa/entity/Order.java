package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a merchant's order placed through IPOS-SA-ORD.
 *
 * Order lifecycle:
 *   ACCEPTED   – order received; stock reserved; invoice generated.
 *   PROCESSING – being picked / packed by the warehouse.
 *   DISPATCHED – handed to courier; dispatch details populated.
 *   DELIVERED  – confirmed delivered to the merchant.
 *   CANCELLED  – order cancelled before dispatch.
 *
 * Discount logic:
 *   - For FIXED plans: discount_amount = subtotal_amount * fixed_rate / 100 (applied per order).
 *   - For FLEXIBLE plans: discount is calculated at month-end on total monthly spend and
 *     either refunded by cheque or deducted from the next order.
 *   - final_amount = subtotal_amount - discount_amount.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Column(name = "order_id", length = 20)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    /** Sum of all line totals before discount. */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /** Discount deducted at point of order (FIXED plan) or month-end credit (FLEXIBLE plan). */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** Amount invoiced: subtotal_amount - discount_amount. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ── Dispatch details (populated when status transitions to DISPATCHED) ──


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by_user_id")
    private User dispatchedByUser;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;

    @Column(name = "courier", length = 100)
    private String courier;

    @Column(name = "courier_ref", length = 100)
    private String courierRef;

    @Column(name = "expected_delivery")
    private LocalDateTime expectedDelivery;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Invoice invoice;

    public enum OrderStatus {
        ACCEPTED, PROCESSING, DISPATCHED, DELIVERED, CANCELLED
    }
}