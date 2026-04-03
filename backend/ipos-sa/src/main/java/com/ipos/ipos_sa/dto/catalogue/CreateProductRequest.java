package com.ipos.ipos_sa.dto.catalogue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/catalogue (add a new product).
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Description is required")
    private String description;

    private String packageType;
    private String unit;

    @NotNull(message = "Units per pack is required")
    private Integer unitsPerPack;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    @NotNull(message = "Initial availability is required")
    @PositiveOrZero
    private Integer availability;

    @NotNull(message = "Minimum stock level is required")
    @PositiveOrZero
    private Integer minStockLevel;

    /** Defaults to 10% if not provided. */
    private BigDecimal reorderBufferPct;
}
