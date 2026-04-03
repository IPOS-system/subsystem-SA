package com.ipos.ipos_sa.dto.discount;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/discount-plans/fixed.
 * Creates a fixed-rate discount plan where the same percentage applies to every order.
 */
@Data
public class CreateFixedPlanRequest {

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotNull(message = "Fixed rate is required")
    @DecimalMin(value = "0.00", message = "Rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Rate cannot exceed 100%")
    private BigDecimal fixedRate;
}
