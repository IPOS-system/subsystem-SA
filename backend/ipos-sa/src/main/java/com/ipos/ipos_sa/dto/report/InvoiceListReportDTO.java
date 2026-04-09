package com.ipos.ipos_sa.dto.report;

import com.ipos.ipos_sa.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * RPT-04: Invoice list for a specific merchant within a date range. RPT-05: All invoices across all
 * merchants within a date range.
 *
 * <p>Both reports share the same structure — RPT-04 is merchant-scoped, RPT-05 has
 * merchantId/merchantName populated on each line.
 *
 * <p>Returned by: GET /api/reports/invoice-list?merchantId=&from=&to= (RPT-04) GET
 * /api/reports/all-invoices?from=&to= (RPT-05)
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

    /** Populated on RPT-05 (all merchants); null on RPT-04. */
    private Integer merchantId;

    private String merchantName;

    private LocalDateTime invoiceDate;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal totalPaid;
    private Invoice.PaymentStatus paymentStatus;
  }
}
