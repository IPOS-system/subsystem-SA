package com.ipos.ipos_sa.dto.discount;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * Request body for POST /api/discount-plans/flexible. Creates a tiered discount plan where the rate
 * depends on total monthly spend.
 *
 * <p>Example tiers from the brief: 1% for orders under £1000 2% for orders £1000–£2000 3% for
 * orders over £2000
 *
 * <p>The last tier should have maxOrderVal as null to represent "no upper limit".
 */
@Data
public class CreateFlexiblePlanRequest {

  @NotBlank(message = "Plan name is required")
  private String planName;

  @NotEmpty(message = "At least one tier is required")
  private List<TierRequest> tiers;

  @Data
  public static class TierRequest {

    @NotNull(message = "Minimum order value is required")
    @PositiveOrZero
    private BigDecimal minOrderVal;

    /** Null on the final tier to represent open-ended upper bound. */
    private BigDecimal maxOrderVal;

    @NotNull(message = "Discount rate is required")
    @DecimalMin(value = "0.00", message = "Rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Rate cannot exceed 100%")
    private BigDecimal discountRate;
  }
}
