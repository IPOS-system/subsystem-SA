package com.ipos.ipos_sa.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body for PUT /api/orders/{id}/dispatch.
 * All five dispatch fields are required by the brief when marking an order as DISPATCHED.
 */
@Data
public class DispatchOrderRequest {

    @NotBlank(message = "Courier name is required")
    private String courier;

    @NotBlank(message = "Courier reference number is required")
    private String courierRef;

    @NotNull(message = "Dispatch date is required")
    private LocalDateTime dispatchDate;

    @NotNull(message = "Expected delivery date is required")
    private LocalDateTime expectedDelivery;
}
