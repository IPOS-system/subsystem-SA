package com.ipos.ipos_sa.dto.account;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for PUT /api/accounts/merchant/{id}. All fields are optional — only non-null values
 * will be applied. Credit limit and discount plan changes require ADMIN or MANAGER role (enforced
 * in the service layer).
 */
@Data
public class UpdateMerchantRequest {

  private String companyName;
  private String address;
  private String phone;
  private String fax;
  private String email;

  /** ADMIN / MANAGER only. */
  private BigDecimal creditLimit;

  /** ADMIN / MANAGER only. Must reference an existing discount plan ID. */
  private Integer discountPlanId;
}
