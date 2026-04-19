package com.ipos.ipos_sa.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified response for POST /api/orders used by inter-subsystem communication (CA).
 *
 * Returns a clear success/failure indicator with a reason on failure. On success,
 * the orderId is included so the calling subsystem can track the order.
 *
 * Failure reasons include:
 *   ACCOUNT_SUSPENDED — merchant account is suspended, no orders accepted
 *   ACCOUNT_IN_DEFAULT — merchant account is in default, no orders accepted
 *   INSUFFICIENT_STOCK — one or more products have insufficient stock
 *   PRODUCT_NOT_FOUND — a requested product ID does not exist
 *   PRODUCT_INACTIVE — a requested product is no longer active
 *   CREDIT_LIMIT_EXCEEDED — order would exceed the merchant's credit limit
 *   VALIDATION_ERROR — request body is invalid (missing items, bad quantity, etc.)
 *   INTERNAL_ERROR — unexpected server error
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {

  /** True if the order was placed successfully. */
  private boolean success;

  /** The generated order ID only present on success, null on failure. */
  private String orderId;

  /** Message explaining the result. */
  private String message;

  /** Error code null on success. */
  private String errorCode;

  // Factory methods for clean controller code

  public static PlaceOrderResponse success(String orderId) {
    return PlaceOrderResponse.builder()
        .success(true)
        .orderId(orderId)
        .message("Order placed successfully")
        .errorCode(null)
        .build();
  }

  public static PlaceOrderResponse failure(String errorCode, String message) {
    return PlaceOrderResponse.builder()
        .success(false)
        .orderId(null)
        .message(message)
        .errorCode(errorCode)
        .build();
  }
}