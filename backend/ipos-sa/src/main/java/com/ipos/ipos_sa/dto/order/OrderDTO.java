package com.ipos.ipos_sa.dto.order;

import com.ipos.ipos_sa.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for an order. Returned by GET /api/orders/{id} and GET
 * /api/orders/merchant/{merchantId}. Dispatch fields are null until the order status is DISPATCHED.
 */
@Data
@Builder
public class OrderDTO {

  private String orderId;
  private Integer merchantId;
  private String merchantName;

  private LocalDateTime orderDate;
  private Order.OrderStatus status;

  private BigDecimal subtotal;
  private BigDecimal discountAmount;
  private BigDecimal totalAmount;

  private List<OrderItemDTO> items;

  // Dispatch details — null until status = DISPATCHED
  private String dispatchedByUsername;
  private LocalDateTime dispatchDate;
  private String courier;
  private String courierRef;
  private LocalDateTime expectedDelivery;
}
