package com.ipos.ipos_sa.dto.discount;

import com.ipos.ipos_sa.entity.DiscountPlan;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a discount plan. Returned by GET /api/discount-plans. Used o populate dropdowns
 * on account creation and edit screens.t
 */
@Data
@Builder
public class DiscountPlanDTO {

  private Integer planId;
  private String planName;
  private DiscountPlan.PlanType planType;

  /** Populated for fixed plans, null for flexible. */
  private BigDecimal fixedRate;

  /** Populated for flexible plans, empty for fixed. */
  private List<TierDTO> tiers;

  @Data
  @Builder
  public static class TierDTO {
    private Integer tierId;
    private BigDecimal minOrderVal;

    /** Null on the final tier and represents no upper bound. */
    private BigDecimal maxOrderVal;

    private BigDecimal discountRate;
  }
}
