package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.invoice.InvoiceDTO;
import com.ipos.ipos_sa.dto.payment.RecordPaymentRequest;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final InvoiceRepository invoiceRepository;
  private final MerchantRepository merchantRepository;
  private final AuditLogRepository auditLogRepository;

  // ── Record Payment (UC-20, UC-41) ─────────────────────────────────────────

  /**
   * Records a payment made by a merchant against a specific invoice.
   *
   * <p>Per the brief: "Payments by merchants are not part of IPOS-SA and should be made using other
   * means, such as direct bank transfers." Once a payment is received externally, the accounting
   * department enters it here.
   *
   * <p>This method performs the following (UC-41 inclusions): 1. Validates the invoice exists and
   * belongs to the specified merchant. 2. Validates the payment amount does not exceed what is
   * owed. 3. Creates the Payment record. 4. Reduces the invoice's amount_due (UC-42 via invoice).
   * 5. If fully settled, marks the invoice as PAID. 6. Reduces the merchant's current_balance
   * (UC-42). 7. If the merchant was SUSPENDED and no more overdue invoices remain, restores the
   * account to NORMAL (UC-43).
   *
   * @param request payment details entered by the accountant
   * @param actingUser the authenticated ACCOUNTANT or ADMIN user
   * @return the updated invoice as a DTO
   */
  @Transactional
  public InvoiceDTO recordPayment(RecordPaymentRequest request, User actingUser) {

    // ── 1. Load and validate invoice ──────────────────────────────────────
    Invoice invoice =
        invoiceRepository
            .findById(request.getInvoiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", request.getInvoiceId()));

    // Verify the invoice belongs to the specified merchant
    if (!invoice.getMerchant().getMerchantId().equals(request.getMerchantId())) {
      throw new ValidationException(
          "Invoice "
              + request.getInvoiceId()
              + " does not belong to merchant "
              + request.getMerchantId()
              + ".");
    }

    // Cannot pay an already-paid invoice
    if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PAID) {
      throw new ValidationException(
          "Invoice " + request.getInvoiceId() + " is already fully paid.");
    }

    // ── 2. Validate amount ────────────────────────────────────────────────
    if (request.getAmountPaid().compareTo(invoice.getAmountDue()) > 0) {
      throw new ValidationException(
          "Payment amount (£"
              + request.getAmountPaid()
              + ") exceeds amount due (£"
              + invoice.getAmountDue()
              + ").");
    }

    // ── 3. Create Payment record ──────────────────────────────────────────
    Merchant merchant = invoice.getMerchant();

    Payment payment =
        Payment.builder()
            .merchant(merchant)
            .invoice(invoice)
            .amountPaid(request.getAmountPaid())
            .paymentMethod(request.getPaymentMethod())
            .paymentDate(request.getPaymentDate())
            .notes(request.getNotes())
            .recordedBy(actingUser)
            .build();

    paymentRepository.save(payment);

    // ── 4. Update invoice outstanding balance ─────────────────────────────
    BigDecimal newAmountDue = invoice.getAmountDue().subtract(request.getAmountPaid());
    invoice.setAmountDue(newAmountDue);

    // ── 5. Update invoice payment status ──────────────────────────────────
    if (newAmountDue.compareTo(BigDecimal.ZERO) <= 0) {
      invoice.setAmountDue(BigDecimal.ZERO);
      invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
    }
    // Note: the schema only has PENDING / PAID / OVERDUE — we keep it
    // as PENDING (or OVERDUE) until fully paid, which is consistent
    // with the brief's "payment received (or pending)" wording.

    invoiceRepository.save(invoice);

    // ── 6. Update merchant balance (UC-42) ────────────────────────────────
    merchant.setCurrentBalance(merchant.getCurrentBalance().subtract(request.getAmountPaid()));

    // Guard against negative balance from rounding
    if (merchant.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
      merchant.setCurrentBalance(BigDecimal.ZERO);
    }

    // ── 7. Auto-restore from SUSPENDED if applicable (UC-43) ─────────────
    //
    // Per the brief: "Once a payment is received within 30 days of the
    // payment deadline the 'suspended' account is restored to 'normal'
    // by the system and the user can resume placing new orders."
    //
    // We check if the merchant has any remaining unpaid invoices.
    // If none remain, restore to NORMAL. IN_DEFAULT is NOT auto-restored
    // — that requires Director authorisation (already handled in
    // AccountService.restoreFromDefault).

    if (merchant.getAccountStatus() == Merchant.AccountStatus.SUSPENDED) {
      List<Invoice> stillUnpaid =
          invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(merchant.getMerchantId());

      if (stillUnpaid.isEmpty()) {
        merchant.setAccountStatus(Merchant.AccountStatus.NORMAL);
        merchant.setStatusChangedAt(LocalDateTime.now());
        log.info(
            "Merchant {} auto-restored from SUSPENDED to NORMAL after payment",
            merchant.getMerchantId());
      }
    }

    merchantRepository.save(merchant);

    // ── 8. Audit log ──────────────────────────────────────────────────────
    audit(
        actingUser,
        "RECORD_PAYMENT",
        "payment",
        String.valueOf(payment.getPaymentId()),
        "Payment of £"
            + request.getAmountPaid()
            + " recorded against invoice "
            + request.getInvoiceId()
            + " for merchant "
            + merchant.getCompanyName()
            + " via "
            + request.getPaymentMethod());

    log.info(
        "Payment recorded: £{} against invoice {} for merchantId={}",
        request.getAmountPaid(),
        request.getInvoiceId(),
        merchant.getMerchantId());

    return toInvoiceDTO(invoice);
  }

  // ── Invoice Queries (UC-18, UC-19) ────────────────────────────────────────

  /**
   * Returns all invoices for a merchant. Used by the "My Balance" screen on the merchant dashboard
   * and the Accountant's invoice list filtered by merchant.
   */
  public List<InvoiceDTO> getInvoicesByMerchant(Integer merchantId) {
    return invoiceRepository.findByMerchant_MerchantId(merchantId).stream()
        .map(this::toInvoiceDTO)
        .collect(Collectors.toList());
  }

  /**
   * Returns a single invoice by ID with payment history. Used when clicking into an invoice from
   * the list view.
   */
  public InvoiceDTO getInvoiceById(String invoiceId) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    return toInvoiceDTO(invoice);
  }

  /**
   * Returns all unpaid invoices for a merchant. Used by the Accountant's "Record Payment" screen to
   * populate the invoice dropdown — only invoices with an outstanding balance are shown.
   */
  public List<InvoiceDTO> getUnpaidInvoicesByMerchant(Integer merchantId) {
    return invoiceRepository
        .findByMerchant_MerchantIdAndPaymentStatusNot(merchantId, Invoice.PaymentStatus.PAID)
        .stream()
        .map(this::toInvoiceDTO)
        .collect(Collectors.toList());
  }

  // ── Mapper ────────────────────────────────────────────────────────────────

  private InvoiceDTO toInvoiceDTO(Invoice invoice) {

    // Calculate total paid from all payment records
    BigDecimal totalPaid = paymentRepository.sumAmountPaidByInvoiceId(invoice.getInvoiceId());

    // Build payment summaries
    List<InvoiceDTO.PaymentSummaryDTO> paymentSummaries =
        paymentRepository.findByInvoice_InvoiceId(invoice.getInvoiceId()).stream()
            .map(
                p ->
                    InvoiceDTO.PaymentSummaryDTO.builder()
                        .paymentId(p.getPaymentId())
                        .amountPaid(p.getAmountPaid())
                        .paymentMethod(p.getPaymentMethod().name())
                        .paymentDate(p.getPaymentDate())
                        .notes(p.getNotes())
                        .build())
            .collect(Collectors.toList());

    return InvoiceDTO.builder()
        .invoiceId(invoice.getInvoiceId())
        .orderId(invoice.getOrder().getOrderId())
        .merchantId(invoice.getMerchant().getMerchantId())
        .merchantName(invoice.getMerchant().getCompanyName())
        .invoiceDate(invoice.getInvoiceDate())
        .dueDate(invoice.getDueDate())
        .amountDue(invoice.getAmountDue())
        .totalPaid(totalPaid)
        .paymentStatus(invoice.getPaymentStatus())
        .payments(paymentSummaries)
        .build();
  }

  // ── Audit Helper ──────────────────────────────────────────────────────────

  private void audit(
      User actor, String action, String targetType, String targetId, String details) {
    AuditLog entry =
        AuditLog.builder()
            .user(actor)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .details(details)
            .build();
    auditLogRepository.save(entry);
  }
}
