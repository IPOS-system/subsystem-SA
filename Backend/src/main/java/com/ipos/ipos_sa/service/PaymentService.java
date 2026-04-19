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

  // Record Payment

  /**
   * Records a payment made by a merchant against a specific invoice.
   *
   */
  @Transactional
  public InvoiceDTO recordPayment(RecordPaymentRequest request, User actingUser) {

    // 1. Load and validate invoice
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

    // 2. Validate amount
    if (request.getAmountPaid().compareTo(invoice.getAmountDue()) > 0) {
      throw new ValidationException(
          "Payment amount (£"
              + request.getAmountPaid()
              + ") exceeds amount due (£"
              + invoice.getAmountDue()
              + ").");
    }

    // 3. Create Payment record
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

    // 4. Update invoice outstanding balance
    BigDecimal newAmountDue = invoice.getAmountDue().subtract(request.getAmountPaid());
    invoice.setAmountDue(newAmountDue);

    // 5. Update invoice payment status
    if (newAmountDue.compareTo(BigDecimal.ZERO) <= 0) {
      invoice.setAmountDue(BigDecimal.ZERO);
      invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
    }

    invoiceRepository.save(invoice);

    // 6. Update merchant balance
    merchant.setCurrentBalance(merchant.getCurrentBalance().subtract(request.getAmountPaid()));
    if (merchant.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
      merchant.setCurrentBalance(BigDecimal.ZERO);
    }

    // 7. Auto-restore from SUSPENDED if applicable 
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

    // 8. Audit log
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

  // Invoice Queries

  /**
   * Returns all invoices for a merchant.
   */
  public List<InvoiceDTO> getInvoicesByMerchant(Integer merchantId) {
    return invoiceRepository.findByMerchant_MerchantId(merchantId).stream()
        .map(this::toInvoiceDTO)
        .collect(Collectors.toList());
  }

  /**
   * Returns a single invoice by ID with payment history.
   */
  public InvoiceDTO getInvoiceById(String invoiceId) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    return toInvoiceDTO(invoice);
  }

  /**
   * Returns all unpaid invoices for a merchant.
   */
  public List<InvoiceDTO> getUnpaidInvoicesByMerchant(Integer merchantId) {
    return invoiceRepository
        .findByMerchant_MerchantIdAndPaymentStatusNot(merchantId, Invoice.PaymentStatus.PAID)
        .stream()
        .map(this::toInvoiceDTO)
        .collect(Collectors.toList());
  }

  // Mapper

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

  // Audit Helper

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
