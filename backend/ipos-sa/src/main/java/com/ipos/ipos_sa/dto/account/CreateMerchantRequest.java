package com.ipos.ipos_sa.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/accounts/merchant.
 * All required fields must be present — the account will not be created
 * if any are missing, per the brief (IPOS-SA-ACC).
 */
@Data
public class CreateMerchantRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String fax;

    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Credit limit is required")
    @Positive(message = "Credit limit must be positive")
    private BigDecimal creditLimit;

    /** ID of the discount plan to assign. Must reference an existing plan. */
    @NotNull(message = "Discount plan is required")
    private Integer discountPlanId;
}
