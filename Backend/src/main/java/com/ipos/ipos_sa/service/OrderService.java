package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.order.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final CatalogueItemRepository catalogueItemRepository;
  private final MerchantRepository merchantRepository;
  private final InvoiceRepository invoiceRepository;
  private final MonthlyDiscountTrackerRepository trackerRepository;
  private final AuditLogRepository auditLogRepository;

  // Place Order

  /**
   * Places a new order for a merchant.
   *
   * Business rules enforced: 1. Merchant account must be NORMAL — SUSPENDED and
   * IN_DEFAULT are blocked entirely. 2. Each product must exist and be active. 3. Sufficient stock
   * must be available for each line item. 4. Order total (after discount) must not exceed the
   * merchant's remaining credit (credit_limit - current_balance). 5. For FIXED discount plans:
   * discount is applied immediately per order. 6. For FLEXIBLE discount plans: no per-order
   * discount; monthly total is tracked and discount is calculated at month-end. 7. Stock is
   * decremented on acceptance. 8. An invoice is generated automatically with due date = end of
   * current calendar month. 9. The merchant's current_balance is increased by the invoiced amount.
   *
   */
  @Transactional
  public OrderDTO placeOrder(Integer merchantId, PlaceOrderRequest request, User actingUser) {

    // 1. Load and validate merchant
    Merchant merchant =
        merchantRepository
            .findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

    if (merchant.getAccountStatus() != Merchant.AccountStatus.NORMAL) {
      throw new ValidationException(
          "Merchant account is " + merchant.getAccountStatus() + " — no new orders accepted.");
    }

    // 2. Generate order ID
    String orderId = generateOrderId();

    // 3. Build line items, validate stock, calculate subtotal
    List<OrderItem> orderItems = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;

    for (PlaceOrderRequest.OrderLineRequest line : request.getItems()) {

      // Normalize product ID
      String normalizedId = line.getProductId().replace("-", " ");

      CatalogueItem product =
          catalogueItemRepository
              .findById(normalizedId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("CatalogueItem", normalizedId));

      if (!product.getIsActive()) {
        throw new ValidationException(
            "Product " + line.getProductId() + " is not available for ordering.");
      }

      if (product.getAvailability() < line.getQuantity()) {
        throw new ValidationException(
            "Insufficient stock for "
                + product.getDescription()
                + ": requested "
                + line.getQuantity()
                + ", available "
                + product.getAvailability());
      }

      BigDecimal lineTotal =
          product.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));

      OrderItem item =
          OrderItem.builder()
              .catalogueItem(product)
              .quantity(line.getQuantity())
              .unitPrice(product.getUnitPrice())
              .lineTotal(lineTotal)
              .build();

      orderItems.add(item);
      subtotal = subtotal.add(lineTotal);
    }

    // 4. Calculate discount
    BigDecimal discountAmount = BigDecimal.ZERO;
    DiscountPlan plan = merchant.getDiscountPlan();

    if (plan != null && plan.getPlanType() == DiscountPlan.PlanType.FIXED) {
      // FIXED: discount applied immediately per order
      discountAmount =
          subtotal
              .multiply(plan.getFixedRate())
              .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    // FLEXIBLE: no per-order discount tracked at month-end

    BigDecimal totalAmount = subtotal.subtract(discountAmount);

    // 5. Credit limit check
    BigDecimal remainingCredit = merchant.getCreditLimit().subtract(merchant.getCurrentBalance());

    if (totalAmount.compareTo(remainingCredit) > 0) {
      throw new ValidationException(
          "Order total (£"
              + totalAmount
              + ") exceeds remaining credit (£"
              + remainingCredit
              + "). Credit limit: £"
              + merchant.getCreditLimit()
              + ", current balance: £"
              + merchant.getCurrentBalance()
              + ".");
    }

    // 6. Create the order
    Order order =
        Order.builder()
            .orderId(orderId)
            .merchant(merchant)
            .orderDate(LocalDateTime.now())
            .status(Order.OrderStatus.ACCEPTED)
            .subtotalAmount(subtotal)
            .discountAmount(discountAmount)
            .totalAmount(totalAmount)
            .orderItems(new ArrayList<>())
            .build();

    orderRepository.save(order);

    // Link items to the saved order and save them
    for (OrderItem item : orderItems) {
      item.setOrder(order);
    }
    orderItemRepository.saveAll(orderItems);
    order.setOrderItems(orderItems);

    // 7. Deduct stock
    for (OrderItem item : orderItems) {
      CatalogueItem product = item.getCatalogueItem();
      product.setAvailability(product.getAvailability() - item.getQuantity());
      catalogueItemRepository.save(product);
    }

    // 8. Generate invoice
    LocalDate dueDate = YearMonth.now().atEndOfMonth();

    String invoiceId = "INV-" + orderId;
    Invoice invoice =
        Invoice.builder()
            .invoiceId(invoiceId)
            .order(order)
            .merchant(merchant)
            .invoiceDate(LocalDateTime.now())
            .dueDate(dueDate)
            .amountDue(totalAmount)
            .paymentStatus(Invoice.PaymentStatus.PENDING)
            .build();

    invoiceRepository.save(invoice);

    // 9. Update merchant balance
    merchant.setCurrentBalance(merchant.getCurrentBalance().add(totalAmount));
    merchantRepository.save(merchant);

    // 10. Track monthly spend for FLEXIBLE discount
    if (plan != null && plan.getPlanType() == DiscountPlan.PlanType.FLEXIBLE) {
      String yearMonth = YearMonth.now().toString();

      MonthlyDiscountTracker tracker =
          trackerRepository
              .findByMerchant_MerchantIdAndYearMonth(merchantId, yearMonth)
              .orElse(
                  MonthlyDiscountTracker.builder()
                      .merchant(merchant)
                      .yearMonth(yearMonth)
                      .totalOrderValue(BigDecimal.ZERO)
                      .settled(false)
                      .build());

      tracker.setTotalOrderValue(tracker.getTotalOrderValue().add(subtotal));
      trackerRepository.save(tracker);
    }

    // 11. Audit log
    audit(
        actingUser,
        "PLACE_ORDER",
        "order",
        orderId,
        "Order placed: £"
            + totalAmount
            + " ("
            + orderItems.size()
            + " items) for merchant "
            + merchant.getCompanyName());

    log.info("Order placed: {} for merchantId={} total=£{}", orderId, merchantId, totalAmount);

    return toDTO(order);
  }

  // Update Order Status

  /**
   * Updates the status of an order. Validates that the transition is legal: ACCEPTED → PROCESSING →
   * DISPATCHED → DELIVERED ACCEPTED → CANCELLED (only before dispatch)
   *
   * For DISPATCHED, use the dispatchOrder() method instead.
   */
  @Transactional
  public OrderDTO updateOrderStatus(String orderId, Order.OrderStatus newStatus, User actingUser) {

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

    validateStatusTransition(order.getStatus(), newStatus);

    // If cancelling, restore stock and reverse balance
    if (newStatus == Order.OrderStatus.CANCELLED) {
      cancelOrder(order);
    }

    order.setStatus(newStatus);
    orderRepository.save(order);

    audit(actingUser, "UPDATE_ORDER_STATUS", "order", orderId, "Status changed to " + newStatus);

    log.info("Order {} status updated to {}", orderId, newStatus);
    return toDTO(order);
  }

  // Dispatch Order

  /**
   * Marks an order as DISPATCHED and records the courier details.
   */
  @Transactional
  public OrderDTO dispatchOrder(String orderId, DispatchOrderRequest request, User actingUser) {

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

    validateStatusTransition(order.getStatus(), Order.OrderStatus.DISPATCHED);

    order.setStatus(Order.OrderStatus.DISPATCHED);
    order.setDispatchedByUser(actingUser);
    order.setDispatchDate(request.getDispatchDate());
    order.setCourier(request.getCourier());
    order.setCourierRef(request.getCourierRef());
    order.setExpectedDelivery(request.getExpectedDelivery());

    orderRepository.save(order);

    audit(
        actingUser,
        "DISPATCH_ORDER",
        "order",
        orderId,
        "Dispatched via " + request.getCourier() + " ref=" + request.getCourierRef());

    log.info("Order {} dispatched by userId={}", orderId, actingUser.getUserId());
    return toDTO(order);
  }

  // Track Order

  /**
   * Returns a single order by ID.
   */
  public OrderDTO getOrderById(String orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    return toDTO(order);
  }

  // View Order History

  /**
   * Returns all orders for a merchant, newest first.
   */
  public List<OrderDTO> getOrdersByMerchant(Integer merchantId) {
    return orderRepository.findByMerchant_MerchantIdOrderByOrderDateDesc(merchantId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // Admin: All Orders by Status

  /** Returns all orders filtered by status. */
  public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status) {
    return orderRepository.findByStatus(status).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Returns all orders across all merchants.
   */
  public List<OrderDTO> getAllOrders() {
    return orderRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Returns all incomplete orders (not DELIVERED, not CANCELLED).
   */
  public List<OrderDTO> getIncompleteOrders() {
    return orderRepository
        .findByStatusNotIn(List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.CANCELLED))
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // Cancel Order

  /**
   * Handles the side-effects of cancelling an order: 1. Restore stock for each line item. 2.
   * Reverse the merchant's balance increase. 3. Mark the associated invoice as PAID (£0 owed) or
   * delete it.
   */
  private void cancelOrder(Order order) {

    // Restore stock
    List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());
    for (OrderItem item : items) {
      CatalogueItem product = item.getCatalogueItem();
      product.setAvailability(product.getAvailability() + item.getQuantity());
      catalogueItemRepository.save(product);
    }

    // Reverse balance
    Merchant merchant = order.getMerchant();
    merchant.setCurrentBalance(merchant.getCurrentBalance().subtract(order.getTotalAmount()));
    merchantRepository.save(merchant);

    // Mark invoice as settled
    Invoice invoice = order.getInvoice();
    if (invoice != null) {
      invoice.setAmountDue(BigDecimal.ZERO);
      invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
      invoiceRepository.save(invoice);
    }

    log.info("Order {} cancelled — stock restored, balance reversed", order.getOrderId());
  }

  // Status Transition Validation

  /**
   * Validates that the requested status transition is legal.
   *
   * <p>Valid transitions: ACCEPTED → PROCESSING, CANCELLED PROCESSING → DISPATCHED, CANCELLED
   * DISPATCHED → DELIVERED DELIVERED → (terminal — no further transitions) CANCELLED → (terminal —
   * no further transitions)
   */
  private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus requested) {
    boolean valid =
        switch (current) {
          case ACCEPTED -> requested == Order.OrderStatus.PROCESSING
              || requested == Order.OrderStatus.CANCELLED;
          case PROCESSING -> requested == Order.OrderStatus.DISPATCHED
              || requested == Order.OrderStatus.CANCELLED;
          case DISPATCHED -> requested == Order.OrderStatus.DELIVERED;
          case DELIVERED, CANCELLED -> false;
        };

    if (!valid) {
      throw new ValidationException(
          "Cannot transition order from " + current + " to " + requested + ".");
    }
  }

  // Order ID Generator

  /**
   * Generates a unique order ID
   */
  private String generateOrderId() {
    LocalDate today = LocalDate.now();
    String dateStr = today.toString().replace("-", "");
    long count = orderRepository.count() + 1;
    return String.format("ORD-%s-%04d", dateStr, count);
  }

  // Mapper

  private OrderDTO toDTO(Order order) {

    List<OrderItemDTO> itemDTOs;

    if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
      itemDTOs = order.getOrderItems().stream().map(this::toItemDTO).collect(Collectors.toList());
    } else {
      itemDTOs =
          orderItemRepository.findByOrder_OrderId(order.getOrderId()).stream()
              .map(this::toItemDTO)
              .collect(Collectors.toList());
    }

    return OrderDTO.builder()
        .orderId(order.getOrderId())
        .merchantId(order.getMerchant().getMerchantId())
        .merchantName(order.getMerchant().getCompanyName())
        .orderDate(order.getOrderDate())
        .status(order.getStatus())
        .subtotal(order.getSubtotalAmount())
        .discountAmount(order.getDiscountAmount())
        .totalAmount(order.getTotalAmount())
        .items(itemDTOs)
        .dispatchedByUsername(
            order.getDispatchedByUser() != null ? order.getDispatchedByUser().getUsername() : null)
        .dispatchDate(order.getDispatchDate())
        .courier(order.getCourier())
        .courierRef(order.getCourierRef())
        .expectedDelivery(order.getExpectedDelivery())
        .build();
  }

  private OrderItemDTO toItemDTO(OrderItem item) {
    return OrderItemDTO.builder()
        .productId(item.getCatalogueItem().getProductId())
        .description(item.getCatalogueItem().getDescription())
        .quantity(item.getQuantity())
        .unitPrice(item.getUnitPrice())
        .lineTotal(item.getLineTotal())
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
