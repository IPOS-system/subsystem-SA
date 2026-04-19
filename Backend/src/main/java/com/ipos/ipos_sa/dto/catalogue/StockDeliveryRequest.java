package com.ipos.ipos_sa.dto.catalogue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request body for POST /api/catalogue/{id}/stock. Records a stock delivery, increasing the
 * product's availability.
 */
@Data
public class StockDeliveryRequest {

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be greater than zero")
  private Integer quantity;
}
