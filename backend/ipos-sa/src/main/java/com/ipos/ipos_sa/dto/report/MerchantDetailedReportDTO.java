package com.ipos.ipos_sa.dto.report;

import com.ipos.ipos_sa.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * RPT-03: Merchant detailed report. Contact details as header, then all orders with full line item
 * breakdown. Matches the layout in Appendix 5 of the brief. Returned by GET
 * /api/reports/merchant-detailed?merchantId=&from=&to=
 */
@Data
@Builder
public class MerchantDetailedReportDTO {

  private LocalDateTime from;
  private LocalDateTime to;

  // Merchant contact header
  private Integer merchantId;
  private String companyName;
  private String address;
  private String phone;
  private String fax;
  private String email;

  private List<DetailedOrderDTO> orders;

  private BigDecimal grandTotal;

  @Data
  @Builder
  public static class DetailedOrderDTO {
    private String orderId;
    private BigDecimal orderTotal;
    private LocalDateTime orderedDate;
    private BigDecimal discountAmount;
    private Invoice.PaymentStatus paymentStatus;
    private List<LineItemDTO> items;
  }

  @Data
  @Builder
  public static class LineItemDTO {
    private String productId;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
  }
}
