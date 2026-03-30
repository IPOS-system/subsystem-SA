package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a merchant (pharmacy / retail client) registered in IPOS-SA.
 * A merchant is linked to exactly one User and may be assigned a DiscountPlan.
 *
 * Account states:
 *   NORMAL      – active; orders accepted.
 *   SUSPENDED   – payment 15–30 days overdue; no new orders.
 *   IN_DEFAULT  – payment > 30 days overdue; requires Director authorisation to restore.
 */
@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Integer merchantId;

    /** The login credentials for this merchant. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "fax", length = 30)
    private String fax;

    /** Maximum credit InfoPharma extends to this merchant. */
    @Column(name = "credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLimit;

    /** Running total of unpaid invoices for this merchant. */
    @Column(name = "current_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentBalance;


    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_plan_id")
    private DiscountPlan discountPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by")
    private User authorizedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
    private List<Invoice> invoices;

    @OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
    private List<Payment> payments;

    public enum AccountStatus {
        NORMAL, SUSPENDED, IN_DEFAULT
    }
}