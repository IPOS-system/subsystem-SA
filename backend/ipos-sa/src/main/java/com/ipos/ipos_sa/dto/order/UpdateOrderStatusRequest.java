package com.ipos.ipos_sa.dto.order;

import com.ipos.ipos_sa.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for PUT /api/orders/{id}/status.
 * Simple status update — use DispatchOrderRequest instead when moving to DISPATCHED.
 */
@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private Order.OrderStatus status;
}
