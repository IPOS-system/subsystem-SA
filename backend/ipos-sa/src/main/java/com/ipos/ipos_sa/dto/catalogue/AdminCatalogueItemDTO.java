package com.ipos.ipos_sa.dto.catalogue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Admin-facing catalogue item response. Returned by GET /api/catalogue/all — includes internal
 * stock management fields that should never be exposed to merchants.
 */
@Data
@Builder
public class AdminCatalogueItemDTO {

  private String productId;
  private String description;
  private String packageType;
  private String unit;
  private Integer unitsPerPack;
  private BigDecimal unitPrice;
  private Integer availability;
  private Boolean isActive;

  /** Threshold below which a low-stock warning is triggered. */
  private Integer minStockLevel;

  /**
   * Buffer percentage used to calculate recommended reorder quantity. Recommended order =
   * minStockLevel * (1 + reorderBufferPct / 100).
   */
  private BigDecimal reorderBufferPct;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
