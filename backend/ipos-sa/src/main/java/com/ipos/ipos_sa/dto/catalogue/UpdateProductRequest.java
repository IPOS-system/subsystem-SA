package com.ipos.ipos_sa.dto.catalogue;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for PUT /api/catalogue/{id}.
 */
@Data
public class UpdateProductRequest {

  private String description;
  private String packageType;
  private String unit;
  private Integer unitsPerPack;
  private BigDecimal unitPrice;
  private Integer minStockLevel;
  private BigDecimal reorderBufferPct;
}
