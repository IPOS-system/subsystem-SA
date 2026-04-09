package com.ipos.ipos_sa.dto.report;

import com.ipos.ipos_sa.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * RPT-02: Merchant orders summary report. List of orders for a specific merchant within a date
 * range, with a totals row. Matches the layout in Appendix 4 of the brief. Returned by GET
 * /api/reports/merchant-summary?merchantId=&from=&to=
 */
@Data
@Builder
public class MerchantOrdersSummaryDTO {

  private LocalDateTime from;
  private LocalDateTime to;

  // Merchant contact details (header section of the report)
  private Integer merchantId;
  private String companyName;
  private String address;
  private String phone;
  private String fax;

  private List<OrderSummaryLineDTO> orders;

  // Totals row
  private Integer totalOrders;
  private BigDecimal totalValue;
  private Integer totalDispatched;
  private Integer totalDelivered;
  private Integer totalPaid;

  @Data
  @Builder
  public static class OrderSummaryLineDTO {
    private String orderId;
    private LocalDateTime orderedDate;
    private BigDecimal amount;
    private LocalDateTime dispatchedDate;
    private LocalDateTime deliveredDate;
    private Invoice.PaymentStatus paymentStatus;
  }
}
