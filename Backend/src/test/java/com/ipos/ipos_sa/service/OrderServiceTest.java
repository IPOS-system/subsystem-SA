package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.order.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
  @Mock OrderRepository orderRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock CatalogueItemRepository catalogueItemRepository;
  @Mock MerchantRepository merchantRepository;
  @Mock InvoiceRepository invoiceRepository;
  @Mock MonthlyDiscountTrackerRepository trackerRepository;
  @Mock AuditLogRepository auditLogRepository;

  @InjectMocks OrderService orderService;

  User user;
  Merchant merchant;
  CatalogueItem paracetamol;
  CatalogueItem aspirin;

  @BeforeEach
  void setup() {
    user = User.builder().userId(1).username("sysdba").role(User.Role.ADMIN).isActive(true).build();

    DiscountPlan fixed = DiscountPlan.builder()
        .planId(1).planName("Fixed 3%")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("3.00")).build();
    merchant = Merchant.builder()
        .merchantId(10)
        .user(User.builder().userId(2).username("city").build())
        .companyName("CityPharmacy")
        .creditLimit(new BigDecimal("10000"))
        .currentBalance(BigDecimal.ZERO)
        .accountStatus(Merchant.AccountStatus.NORMAL)
        .discountPlan(fixed).build();
    paracetamol = CatalogueItem.builder()
        .productId("100 00001").description("Paracetamol")
        .unitsPerPack(20).unitPrice(new BigDecimal("0.10"))
        .availability(10_000).minStockLevel(300)
        .reorderBufferPct(new BigDecimal("10")).isActive(true).build();

    aspirin = CatalogueItem.builder()
        .productId("100 00002").description("Aspirin")
        .unitsPerPack(20).unitPrice(new BigDecimal("0.50"))
        .availability(12_000).minStockLevel(500)
        .reorderBufferPct(new BigDecimal("10")).isActive(true).build();}

  PlaceOrderRequest request(String productId, int qty) {
    PlaceOrderRequest r = new PlaceOrderRequest();
    PlaceOrderRequest.OrderLineRequest line = new PlaceOrderRequest.OrderLineRequest();
    line.setProductId(productId);
    line.setQuantity(qty);
    r.setItems(List.of(line));
    return r;}

  Order order(Order.OrderStatus status) {
    return Order.builder()
        .orderId("ORD-20260220-0001")
        .merchant(merchant)
        .orderDate(LocalDateTime.now())
        .status(status)
        .subtotalAmount(new BigDecimal("100"))
        .discountAmount(BigDecimal.ZERO)
        .totalAmount(new BigDecimal("100"))
        .build();}

  
  
  
  
  @Test
  void placeOrder_fixedDiscount_appliedPerOrder() {
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(paracetamol));
    when(orderRepository.count()).thenReturn(0L);

    OrderDTO result = orderService.placeOrder(10, request("100 00001", 10), user);

    assertThat(result.getSubtotal()).isEqualByComparingTo("1.00");
    assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.03");
    assertThat(result.getTotalAmount()).isEqualByComparingTo("0.97");
    assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.ACCEPTED);
    assertThat(paracetamol.getAvailability()).isEqualTo(9_990);
    assertThat(merchant.getCurrentBalance()).isEqualByComparingTo("0.97");

    ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceRepository).save(cap.capture());
    assertThat(cap.getValue().getPaymentStatus()).isEqualTo(Invoice.PaymentStatus.PENDING);}

  @Test
  void placeOrder_multiLine_subtotalIsSum() {
    PlaceOrderRequest r = new PlaceOrderRequest();
    PlaceOrderRequest.OrderLineRequest l1 = new PlaceOrderRequest.OrderLineRequest();
    l1.setProductId("100 00001"); l1.setQuantity(10);
    PlaceOrderRequest.OrderLineRequest l2 = new PlaceOrderRequest.OrderLineRequest();
    l2.setProductId("100 00002"); l2.setQuantity(4);
    r.setItems(List.of(l1, l2));

    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(paracetamol));
    when(catalogueItemRepository.findById("100 00002")).thenReturn(Optional.of(aspirin));
    when(orderRepository.count()).thenReturn(0L);

    assertThat(orderService.placeOrder(10, r, user).getSubtotal()).isEqualByComparingTo("3.00");}

  @Test
  void placeOrder_flexiblePlan_noDiscountButTrackerUpdated() {
    merchant.setDiscountPlan(DiscountPlan.builder()
        .planId(2).planName("Tiered")
        .planType(DiscountPlan.PlanType.FLEXIBLE)
        .fixedRate(BigDecimal.ZERO).build());

    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00002")).thenReturn(Optional.of(aspirin));
    when(orderRepository.count()).thenReturn(0L);
    when(trackerRepository.findByMerchant_MerchantIdAndYearMonth(eq(10), anyString()))
        .thenReturn(Optional.empty());

    OrderDTO result = orderService.placeOrder(10, request("100 00002", 100), user);

    assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
    assertThat(result.getTotalAmount()).isEqualByComparingTo("50.00");

    ArgumentCaptor<MonthlyDiscountTracker> cap = ArgumentCaptor.forClass(MonthlyDiscountTracker.class);
    verify(trackerRepository).save(cap.capture());
    assertThat(cap.getValue().getTotalOrderValue()).isEqualByComparingTo("50.00");}

  
  
  
  
  @Test
  void placeOrder_suspendedMerchant_blocked() {
    merchant.setAccountStatus(Merchant.AccountStatus.SUSPENDED);
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));

    assertThatThrownBy(() -> orderService.placeOrder(10, request("100 00001", 5), user))
        .isInstanceOf(ValidationException.class);
    verify(orderRepository, never()).save(any());}

  @Test
  void placeOrder_inDefaultMerchant_blocked() {
    merchant.setAccountStatus(Merchant.AccountStatus.IN_DEFAULT);
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));

    assertThatThrownBy(() -> orderService.placeOrder(10, request("100 00001", 5), user))
        .isInstanceOf(ValidationException.class);}
  @Test
  void placeOrder_insufficientStock_rejected() {
    paracetamol.setAvailability(3);
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(paracetamol));

    assertThatThrownBy(() -> orderService.placeOrder(10, request("100 00001", 100), user))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Insufficient stock");
    assertThat(paracetamol.getAvailability()).isEqualTo(3);}

  
  
  
  @Test
  void placeOrder_inactiveProduct_rejected() {
    paracetamol.setIsActive(false);
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(paracetamol));
    assertThatThrownBy(() -> orderService.placeOrder(10, request("100 00001", 5), user))
        .isInstanceOf(ValidationException.class);}
  @Test
  void placeOrder_unknownProduct_throws() {
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("999 99999")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.placeOrder(10, request("999 99999", 1), user))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void placeOrder_overCreditLimit_rejected() {
    merchant.setCreditLimit(new BigDecimal("1.00"));
    merchant.setCurrentBalance(new BigDecimal("0.50"));
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00002")).thenReturn(Optional.of(aspirin));

    assertThatThrownBy(() -> orderService.placeOrder(10, request("100 00002", 10), user))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("exceeds remaining credit");
    verify(orderRepository, never()).save(any());
  }
  
  
  
  
  
  @Test
  void placeOrder_hyphenatedProductId_normalised() {
    when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(paracetamol));
    when(orderRepository.count()).thenReturn(0L);

    orderService.placeOrder(10, request("100-00001", 5), user);

    verify(catalogueItemRepository).findById("100 00001");}
  @Test
  void placeOrder_unknownMerchant_throws() {
    when(merchantRepository.findById(999)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.placeOrder(999, request("x", 1), user))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void updateStatus_acceptedToProcessing() {
    Order o = order(Order.OrderStatus.ACCEPTED);
    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    assertThat(orderService.updateOrderStatus("ORD-20260220-0001", Order.OrderStatus.PROCESSING, user)
        .getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);}
  @Test
  void updateStatus_processingToDispatched_allowed() {
    Order o = order(Order.OrderStatus.PROCESSING);
    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    assertThat(orderService.updateOrderStatus("ORD-20260220-0001", Order.OrderStatus.DISPATCHED, user)
        .getStatus()).isEqualTo(Order.OrderStatus.DISPATCHED);}

  @Test
  void updateStatus_dispatchedToDelivered() {
    Order o = order(Order.OrderStatus.DISPATCHED);
    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    assertThat(orderService.updateOrderStatus("ORD-20260220-0001", Order.OrderStatus.DELIVERED, user)
        .getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);}

  @Test
  void updateStatus_illegalSkip_rejected() {
    Order o = order(Order.OrderStatus.ACCEPTED);
    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    assertThatThrownBy(() -> orderService.updateOrderStatus(
        "ORD-20260220-0001", Order.OrderStatus.DELIVERED, user))
        .isInstanceOf(ValidationException.class);}

  @Test
  void updateStatus_deliveredIsTerminal() {
    Order o = order(Order.OrderStatus.DELIVERED);
    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    assertThatThrownBy(() -> orderService.updateOrderStatus(
        "ORD-20260220-0001", Order.OrderStatus.PROCESSING, user))
        .isInstanceOf(ValidationException.class);}

  
  
  
  
  @Test
  void cancel_restoresStockAndBalance() {
    Order o = order(Order.OrderStatus.ACCEPTED);
    Invoice inv = Invoice.builder()
        .invoiceId("INV-1").amountDue(new BigDecimal("100"))
        .paymentStatus(Invoice.PaymentStatus.PENDING).build();
    o.setInvoice(inv);
    paracetamol.setAvailability(9_950);
    merchant.setCurrentBalance(new BigDecimal("100"));
    OrderItem line = OrderItem.builder().catalogueItem(paracetamol).quantity(50).build();

    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));
    when(orderItemRepository.findByOrder_OrderId("ORD-20260220-0001")).thenReturn(List.of(line));

    orderService.updateOrderStatus("ORD-20260220-0001", Order.OrderStatus.CANCELLED, user);

    assertThat(paracetamol.getAvailability()).isEqualTo(10_000);
    assertThat(merchant.getCurrentBalance()).isEqualByComparingTo("0");
    assertThat(inv.getPaymentStatus()).isEqualTo(Invoice.PaymentStatus.PAID);}

  @Test
  
  
  
  
  
  
  void dispatchOrder_recordsAllCourierDetails() {
    Order o = Order.builder()
        .orderId("ORD-20260220-0001")
        .merchant(merchant)
        .orderDate(LocalDateTime.now())
        .status(Order.OrderStatus.PROCESSING)
        .subtotalAmount(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
        .build();

    DispatchOrderRequest r = new DispatchOrderRequest();
    r.setCourier("DHL");
    r.setCourierRef("DHL-999-ABC");
    r.setDispatchDate(LocalDateTime.of(2026, 2, 23, 10, 0));
    r.setExpectedDelivery(LocalDateTime.of(2026, 2, 24, 17, 0));

    when(orderRepository.findById("ORD-20260220-0001")).thenReturn(Optional.of(o));

    OrderDTO result = orderService.dispatchOrder("ORD-20260220-0001", r, user);

    assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.DISPATCHED);
    assertThat(result.getCourier()).isEqualTo("DHL");
    assertThat(result.getCourierRef()).isEqualTo("DHL-999-ABC");
    assertThat(result.getDispatchedByUsername()).isEqualTo("sysdba");
    assertThat(o.getDispatchedByUser()).isEqualTo(user);
  }

  @Test
  void getIncompleteOrders_excludesDeliveredAndCancelled() {
    Order o = Order.builder()
        .orderId("ORD-X").merchant(merchant)
        .orderDate(LocalDateTime.now()).status(Order.OrderStatus.ACCEPTED)
        .subtotalAmount(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
        .orderItems(Collections.emptyList()).build();

    when(orderRepository.findByStatusNotIn(
        List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.CANCELLED)))
        .thenReturn(List.of(o));

    List<OrderDTO> result = orderService.getIncompleteOrders();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(Order.OrderStatus.ACCEPTED);}
  @Test
  void getOrderById_missing_throws() {
    when(orderRepository.findById("NOPE")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.getOrderById("NOPE"))
        .isInstanceOf(ResourceNotFoundException.class);}}
