package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.invoice.InvoiceDTO;
import com.ipos.ipos_sa.dto.payment.RecordPaymentRequest;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock PaymentRepository paymentRepository;
  @Mock InvoiceRepository invoiceRepository;
  @Mock MerchantRepository merchantRepository;
  @Mock AuditLogRepository auditLogRepository;

  @InjectMocks PaymentService paymentService;
  User accountant;
  Merchant merchant;
  Invoice invoice;
  @BeforeEach
  void setup() {
    accountant = User.builder()
        .userId(3).username("accountant")
        .role(User.Role.ACCOUNTANT).isActive(true).build();
    merchant = Merchant.builder()
        .merchantId(10).companyName("CityPharmacy")
        .user(User.builder().userId(2).build())
        .creditLimit(new BigDecimal("10000"))
        .currentBalance(new BigDecimal("508.60"))
        .accountStatus(Merchant.AccountStatus.NORMAL)
        .build();
    Order order = Order.builder()
        .orderId("ORD-20260220-0001")
        .merchant(merchant).orderDate(LocalDateTime.now())
        .status(Order.OrderStatus.DELIVERED)
        .subtotalAmount(new BigDecimal("508.60"))
        .discountAmount(BigDecimal.ZERO)
        .totalAmount(new BigDecimal("508.60"))
        .build();
    invoice = Invoice.builder()
        .invoiceId("INV-ORD-20260220-0001")
        .order(order).merchant(merchant)
        .invoiceDate(LocalDateTime.now())
        .dueDate(LocalDate.now().plusDays(15))
        .amountDue(new BigDecimal("508.60"))
        .paymentStatus(Invoice.PaymentStatus.PENDING)
        .build();}
  RecordPaymentRequest pay(BigDecimal amount) {
    RecordPaymentRequest r = new RecordPaymentRequest();
    r.setMerchantId(10);
    r.setInvoiceId("INV-ORD-20260220-0001");
    r.setAmountPaid(amount);
    r.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
    r.setPaymentDate(LocalDateTime.now());
    r.setNotes("Ref: TRF-12345");
    return r;}
  void stubReadback() {
    when(paymentRepository.sumAmountPaidByInvoiceId(anyString())).thenReturn(BigDecimal.ZERO);
    when(paymentRepository.findByInvoice_InvoiceId(anyString())).thenReturn(Collections.emptyList());}

  
  
  
  @Test
  void fullPayment_settlesInvoice() {
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    stubReadback();
    InvoiceDTO result = paymentService.recordPayment(pay(new BigDecimal("508.60")), accountant);
    assertThat(result.getPaymentStatus()).isEqualTo(Invoice.PaymentStatus.PAID);
    assertThat(invoice.getAmountDue()).isEqualByComparingTo("0");
    assertThat(merchant.getCurrentBalance()).isEqualByComparingTo("0.00");
    verify(paymentRepository).save(any(Payment.class));}
  @Test
  void partialPayment_invoiceStaysPending() {
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    stubReadback();
    InvoiceDTO result = paymentService.recordPayment(pay(new BigDecimal("200.00")), accountant);
    assertThat(result.getPaymentStatus()).isEqualTo(Invoice.PaymentStatus.PENDING);
    assertThat(invoice.getAmountDue()).isEqualByComparingTo("308.60");
    assertThat(merchant.getCurrentBalance()).isEqualByComparingTo("308.60");}
  @Test
  void payment_balanceNeverNegative() {
    merchant.setCurrentBalance(new BigDecimal("100.00"));
    invoice.setAmountDue(new BigDecimal("500.00"));
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    stubReadback();
    paymentService.recordPayment(pay(new BigDecimal("500.00")), accountant);

    assertThat(merchant.getCurrentBalance()).isEqualByComparingTo("0");}
  @Test
  void suspendedMerchant_autoRestoredOnFullClearance() {
    merchant.setAccountStatus(Merchant.AccountStatus.SUSPENDED);
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(10)).thenReturn(Collections.emptyList());
    stubReadback();

    paymentService.recordPayment(pay(new BigDecimal("508.60")), accountant);

    assertThat(merchant.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);
  }
  @Test
  void suspendedMerchant_staysIfOtherDebtRemains() {
    merchant.setAccountStatus(Merchant.AccountStatus.SUSPENDED);
    Invoice other = Invoice.builder()
        .invoiceId("INV-OTHER").amountDue(new BigDecimal("50"))
        .paymentStatus(Invoice.PaymentStatus.PENDING).build();
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(10)).thenReturn(List.of(other));
    stubReadback();
    paymentService.recordPayment(pay(new BigDecimal("508.60")), accountant);
    assertThat(merchant.getAccountStatus()).isEqualTo(Merchant.AccountStatus.SUSPENDED);}

  
  @Test
  void inDefaultMerchant_neverAutoRestored() {
    merchant.setAccountStatus(Merchant.AccountStatus.IN_DEFAULT);
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    stubReadback();
    paymentService.recordPayment(pay(new BigDecimal("508.60")), accountant);
    assertThat(merchant.getAccountStatus()).isEqualTo(Merchant.AccountStatus.IN_DEFAULT);}

  
  
  
  
  
  @Test
  void payment_unknownInvoice_throws() {
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> paymentService.recordPayment(pay(BigDecimal.TEN), accountant))
        .isInstanceOf(ResourceNotFoundException.class);}

  
  
  
  
  
  @Test
  void payment_wrongMerchant_rejected() {
    RecordPaymentRequest r = pay(BigDecimal.TEN);
    r.setMerchantId(999);
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    assertThatThrownBy(() -> paymentService.recordPayment(r, accountant))
        .isInstanceOf(ValidationException.class);}

  
  
  
  
  @Test
  void payment_alreadyPaid_rejected() {
    invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
    invoice.setAmountDue(BigDecimal.ZERO);
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    assertThatThrownBy(() -> paymentService.recordPayment(pay(BigDecimal.TEN), accountant))
        .isInstanceOf(ValidationException.class);}

  
  
  
  
  
  @Test
  void payment_overpayment_rejected() {
    when(invoiceRepository.findById("INV-ORD-20260220-0001")).thenReturn(Optional.of(invoice));
    assertThatThrownBy(() -> paymentService.recordPayment(pay(new BigDecimal("1000.00")), accountant))
        .isInstanceOf(ValidationException.class);}

  @Test
  void getInvoicesByMerchant_delegates() {
    when(invoiceRepository.findByMerchant_MerchantId(10)).thenReturn(List.of(invoice));
    stubReadback();

    List<InvoiceDTO> result = paymentService.getInvoicesByMerchant(10);

    
    
    
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getInvoiceId()).isEqualTo("INV-ORD-20260220-0001");}
  @Test
  void getInvoiceById_missing_throws() {
    when(invoiceRepository.findById("NO-INV")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> paymentService.getInvoiceById("NO-INV"))
        .isInstanceOf(ResourceNotFoundException.class);}
}
