package com.ipos.ipos_sa.dto.account;

import com.ipos.ipos_sa.entity.Merchant;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a merchant account.
 * Returned by GET /api/accounts/merchants and GET /api/accounts/merchants/{id}.
 * Never exposes password hash or internal JPA relations.
 */
@Data
@Builder
public class MerchantDTO {

    private Integer merchantId;
    private Integer userId;
    private String username;

    private String companyName;
    private String address;
    private String phone;
    private String fax;
    private String email;

    private BigDecimal creditLimit;
    private BigDecimal currentBalance;

    private Merchant.AccountStatus accountStatus;
    private LocalDateTime statusChangedAt;

    /** The assigned discount plan's ID and name — enough for display purposes. */
    private Integer discountPlanId;
    private String discountPlanName;

    private LocalDateTime createdAt;
}
