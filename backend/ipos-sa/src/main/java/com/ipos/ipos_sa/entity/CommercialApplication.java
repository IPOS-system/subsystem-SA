package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores commercial membership applications submitted via IPOS-PU (ICMAAPI).
 *
 * When a member of the public applies through the online platform as a
 * "commercial" customer, IPOS-PU sends the application to IPOS-SA via
 * the submitCA() interface method. The application is stored here for
 * review by an Administrator.
 */
@Entity
@Table(name = "commercial_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "company_reg_no", nullable = false, length = 50)
    private String companyRegNo;

    @Column(name = "directors", nullable = false, length = 255)
    private String directors;

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }
}