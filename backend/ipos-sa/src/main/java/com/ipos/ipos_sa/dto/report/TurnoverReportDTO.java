package com.ipos.ipos_sa.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * RPT-01: Turnover report. Quantity sold and revenue received per product within a date range.
 * Returned by GET /api/reports/turnover?from=&to=
 */
@Data
@Builder
public class TurnoverReportDTO {

  private LocalDateTime from;
  private LocalDateTime to;

  /** Total revenue across all products in the period. */
  private BigDecimal totalRevenue;

  /** Breakdown per product. */
  private List<ProductTurnoverDTO> lines;

  @Data
  @Builder
  public static class ProductTurnoverDTO {
    private String productId;
    private String description;
    private Integer totalQuantitySold;
    private BigDecimal totalRevenue;
  }
}
