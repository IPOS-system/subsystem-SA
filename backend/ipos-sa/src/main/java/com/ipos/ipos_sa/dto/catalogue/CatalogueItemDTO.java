package com.ipos.ipos_sa.dto.catalogue;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * Merchant catalogue item response. Returned by GET /api/catalogue (active products only).
 */
@Data
@Builder
public class CatalogueItemDTO {

  private String productId;
  private String description;
  private String packageType;
  private String unit;
  private Integer unitsPerPack;
  private BigDecimal unitPrice;

  /** Current stock available for ordering. */
  private Integer availability;
}
