package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a user account in IPOS-SA.
 * Roles determine which packages of IPOS-SA the user may access:
 *   ADMIN   – full access including account management (IPOS-SA-ACC)
 *   MANAGER – access to reports (IPOS-SA-RPT) and can modify merchant credit/status
 *   MERCHANT – access to catalogue (IPOS-SA-CAT) and orders (IPOS-SA-ORD)
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Bidirectional link – one user account maps to at most one merchant profile. */
    @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY)
    private Merchant merchant;

    public enum Role {
        ADMIN, MANAGER, MERCHANT
    }
}