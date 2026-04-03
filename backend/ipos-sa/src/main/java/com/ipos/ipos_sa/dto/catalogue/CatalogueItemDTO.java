package com.ipos.ipos_sa.dto.catalogue;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Merchant-facing catalogue item response.
 * Returned by GET /api/catalogue (active products only).
 * Does NOT expose minStockLevel or reorderBufferPct — those are internal.
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
