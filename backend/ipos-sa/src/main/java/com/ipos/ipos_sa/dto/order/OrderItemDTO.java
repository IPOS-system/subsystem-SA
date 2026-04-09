package com.ipos.ipos_sa.dto.order;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/** A single line item within an OrderDTO. */
@Data
@Builder
public class OrderItemDTO {

  private String productId;
  private String description;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal lineTotal;
}
