package com.ipos.ipos_sa.dto.invoice;

import com.ipos.ipos_sa.entity.Invoice;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for an invoice.
 * Returned by GET /api/invoices/{id} and GET /api/invoices/merchant/{merchantId}.
 */
@Data
@Builder
public class InvoiceDTO {

    private String invoiceId;
    private String orderId;
    private Integer merchantId;
    private String merchantName;

    private LocalDateTime invoiceDate;
    private LocalDate dueDate;

    /** Remaining outstanding balance after payments. */
    private BigDecimal amountDue;

    /** Sum of all payments recorded against this invoice. */
    private BigDecimal totalPaid;

    private Invoice.PaymentStatus paymentStatus;

    /** Payment records — populated on detail view, may be empty on list view. */
    private List<PaymentSummaryDTO> payments;

    @Data
    @Builder
    public static class PaymentSummaryDTO {
        private Integer paymentId;
        private BigDecimal amountPaid;
        private String paymentMethod;
        private LocalDateTime paymentDate;
        private String notes;
    }
}
