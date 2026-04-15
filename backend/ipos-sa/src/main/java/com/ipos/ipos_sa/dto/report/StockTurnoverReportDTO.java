package com.ipos.ipos_sa.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * RPT-06: Stock turnover report.
 *
 * <p>Per the brief (IPOS-SA-RPT item vi): "Stock turnover (goods sold and newly received) within a
 * given period of time."
 *
 * <p>For each product, shows quantity sold to merchants during the period and quantity received
 * (stock deliveries recorded via POST /api/catalogue/{id}/stock).
 */
@Data
@Builder
public class StockTurnoverReportDTO {

  private LocalDateTime from;
  private LocalDateTime to;
  private LocalDateTime generatedAt;
  private List<StockTurnoverLineDTO> lines;

  @Data
  @Builder
  public static class StockTurnoverLineDTO {
    private String productId;
    private String description;

    /** Total quantity sold to merchants in this period (from order_items of non-cancelled orders). */
    private int quantitySold;

    /** Revenue from goods sold in this period. */
    private BigDecimal revenueSold;

    /** Current availability in warehouse. */
    private int currentAvailability;
  }
}
