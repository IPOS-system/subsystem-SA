package com.ipos.ipos_sa.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;

/**
 * Request body for POST /api/orders. The merchant's basket is sent as a list of product IDs and
 * quantities. All business logic (stock check, discount, invoice generation) happens server-side.
 */
@Data
public class PlaceOrderRequest {

  @NotEmpty(message = "Order must contain at least one item")
  private List<OrderLineRequest> items;

  @Data
  public static class OrderLineRequest {

    @NotNull(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;
  }
}
