package com.ipos.ipos_sa.dto.catalogue;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for PUT /api/catalogue/{id}.
 * All fields optional — only non-null values are applied.
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
