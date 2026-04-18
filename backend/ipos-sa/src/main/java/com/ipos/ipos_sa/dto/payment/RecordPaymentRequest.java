package com.ipos.ipos_sa.dto.payment;

import com.ipos.ipos_sa.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Request body for POST /api/payments. Submitted by admin or account after receiving a bank
 * transfer, card, or cheque.
 */
@Data
public class RecordPaymentRequest {

  @NotNull(message = "Merchant ID is required")
  private Integer merchantId;

  @NotBlank(message = "Invoice ID is required")
  private String invoiceId;

  @NotNull(message = "Amount paid is required")
  @Positive(message = "Amount paid must be greater than zero")
  private BigDecimal amountPaid;

  @NotNull(message = "Payment method is required")
  private Payment.PaymentMethod paymentMethod;

  @NotNull(message = "Payment date is required")
  private LocalDateTime paymentDate;

  /** Optional reference notes (bank ref, cheque number, etc). */
  private String notes;
}
