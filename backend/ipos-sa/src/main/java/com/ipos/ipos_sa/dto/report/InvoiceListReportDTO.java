package com.ipos.ipos_sa.dto.report;

import com.ipos.ipos_sa.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Invoice list for a specific merchant within a date range. Returned by: GET /api/reports/invoice-list?merchantId=&from=&to=  GET
 * /api/reports/all-invoices?from=&to= 
 */
@Data
@Builder
public class InvoiceListReportDTO {

  private LocalDateTime from;
  private LocalDateTime to;

  private List<InvoiceLineDTO> invoices;

  @Data
  @Builder
  public static class InvoiceLineDTO {
    private String invoiceId;
    private String orderId;

    private Integer merchantId;

    private String merchantName;

    private LocalDateTime invoiceDate;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal totalPaid;
    private Invoice.PaymentStatus paymentStatus;
  }
}
