package com.ipos.ipos_sa.dto.discount;

import com.ipos.ipos_sa.entity.DiscountPlan;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a discount plan. Returned by GET /api/discount-plans. Used to populate dropdowns
 * on account creation and edit screens.
 */
@Data
@Builder
public class DiscountPlanDTO {

  private Integer planId;
  private String planName;
  private DiscountPlan.PlanType planType;

  /** Populated for FIXED plans, null for FLEXIBLE. */
  private BigDecimal fixedRate;

  /** Populated for FLEXIBLE plans, empty for FIXED. */
  private List<TierDTO> tiers;

  @Data
  @Builder
  public static class TierDTO {
    private Integer tierId;
    private BigDecimal minOrderVal;

    /** Null on the final tier — represents no upper bound. */
    private BigDecimal maxOrderVal;

    private BigDecimal discountRate;
  }
}
