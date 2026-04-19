package com.ipos.ipos_sa.dto.report;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Low stock level report. All active products whose availability is below their minimum
 * stock level. Returned by GET /api/reports/low-stock
 */
@Data
@Builder
public class LowStockReportDTO {

  private LocalDateTime generatedAt;

  private List<LowStockLineDTO> items;

  @Data
  @Builder
  public static class LowStockLineDTO {
    private String productId;
    private String description;
    private Integer availability;
    private Integer minStockLevel;

    /**
     * Recommended minimum order quantity. Calculated as: minStockLevel * (1 + reorderBufferPct /
     * 100) - availability.
     */
    private Integer recommendedOrderQty;
  }
}
