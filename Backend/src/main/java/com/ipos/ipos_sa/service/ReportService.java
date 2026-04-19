package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.report.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final InvoiceRepository invoiceRepository;
  private final MerchantRepository merchantRepository;
  private final PaymentRepository paymentRepository;
  private final CatalogueItemRepository catalogueItemRepository;

  // RPT-01: Turnover Report

  /**
   * Generates a turnover report for a given date range.
   */
  public TurnoverReportDTO generateTurnoverReport(LocalDateTime from, LocalDateTime to) {

    BigDecimal totalRevenue = orderRepository.sumRevenueBetween(from, to);

    List<Object[]> rows = orderRepository.findProductSaleTotalsBetween(from, to);

    List<TurnoverReportDTO.ProductTurnoverDTO> lines =
        rows.stream()
            .map(
                row ->
                    TurnoverReportDTO.ProductTurnoverDTO.builder()
                        .productId((String) row[0])
                        .description((String) row[1])
                        .totalQuantitySold(((Number) row[2]).intValue())
                        .totalRevenue((BigDecimal) row[3])
                        .build())
            .collect(Collectors.toList());

    return TurnoverReportDTO.builder()
        .from(from)
        .to(to)
        .totalRevenue(totalRevenue)
        .lines(lines)
        .build();
  }

  // RPT-02: Merchant Orders Summary

  /**
   * Generates a merchant orders summary report.
   */
  public MerchantOrdersSummaryDTO generateMerchantOrdersSummary(
      Integer merchantId, LocalDateTime from, LocalDateTime to) {

    Merchant merchant =
        merchantRepository
            .findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

    List<Order> orders =
        orderRepository.findByMerchant_MerchantIdAndOrderDateBetween(merchantId, from, to);

    int totalDispatched = 0;
    int totalDelivered = 0;
    int totalPaid = 0;
    BigDecimal totalValue = BigDecimal.ZERO;

    List<MerchantOrdersSummaryDTO.OrderSummaryLineDTO> lines = new java.util.ArrayList<>();

    for (Order order : orders) {
      // Determine payment status from the linked invoice
      Invoice.PaymentStatus payStatus = null;
      if (order.getInvoice() != null) {
        payStatus = order.getInvoice().getPaymentStatus();
      }

      lines.add(
          MerchantOrdersSummaryDTO.OrderSummaryLineDTO.builder()
              .orderId(order.getOrderId())
              .orderedDate(order.getOrderDate())
              .amount(order.getTotalAmount())
              .dispatchedDate(order.getDispatchDate())
              .deliveredDate(
                  order.getStatus() == Order.OrderStatus.DELIVERED
                      ? order.getDispatchDate()
                      : null) // best available proxy
              .paymentStatus(payStatus)
              .build());

      totalValue = totalValue.add(order.getTotalAmount());
      if (order.getStatus() == Order.OrderStatus.DISPATCHED
          || order.getStatus() == Order.OrderStatus.DELIVERED) {
        totalDispatched++;
      }
      if (order.getStatus() == Order.OrderStatus.DELIVERED) {
        totalDelivered++;
      }
      if (payStatus == Invoice.PaymentStatus.PAID) {
        totalPaid++;
      }
    }

    return MerchantOrdersSummaryDTO.builder()
        .from(from)
        .to(to)
        .merchantId(merchant.getMerchantId())
        .companyName(merchant.getCompanyName())
        .address(merchant.getAddress())
        .phone(merchant.getPhone())
        .fax(merchant.getFax())
        .orders(lines)
        .totalOrders(orders.size())
        .totalValue(totalValue)
        .totalDispatched(totalDispatched)
        .totalDelivered(totalDelivered)
        .totalPaid(totalPaid)
        .build();
  }

  // RPT-03: Merchant Detailed Report

  /**
   * Generates a detailed merchant report.
   */
  public MerchantDetailedReportDTO generateMerchantDetailedReport(
      Integer merchantId, LocalDateTime from, LocalDateTime to) {

    Merchant merchant =
        merchantRepository
            .findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

    List<Order> orders =
        orderRepository.findByMerchant_MerchantIdAndOrderDateBetween(merchantId, from, to);

    BigDecimal grandTotal = BigDecimal.ZERO;

    List<MerchantDetailedReportDTO.DetailedOrderDTO> orderDTOs = new java.util.ArrayList<>();

    for (Order order : orders) {
      // Fetch line items for this order
      List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());

      List<MerchantDetailedReportDTO.LineItemDTO> itemDTOs =
          items.stream()
              .map(
                  item ->
                      MerchantDetailedReportDTO.LineItemDTO.builder()
                          .productId(item.getCatalogueItem().getProductId())
                          .quantity(item.getQuantity())
                          .unitCost(item.getUnitPrice())
                          .amount(item.getLineTotal())
                          .build())
              .collect(Collectors.toList());

      Invoice.PaymentStatus payStatus = null;
      if (order.getInvoice() != null) {
        payStatus = order.getInvoice().getPaymentStatus();
      }

      orderDTOs.add(
          MerchantDetailedReportDTO.DetailedOrderDTO.builder()
              .orderId(order.getOrderId())
              .orderTotal(order.getTotalAmount())
              .orderedDate(order.getOrderDate())
              .discountAmount(order.getDiscountAmount())
              .paymentStatus(payStatus)
              .items(itemDTOs)
              .build());

      grandTotal = grandTotal.add(order.getTotalAmount());
    }

    return MerchantDetailedReportDTO.builder()
        .from(from)
        .to(to)
        .merchantId(merchant.getMerchantId())
        .companyName(merchant.getCompanyName())
        .address(merchant.getAddress())
        .phone(merchant.getPhone())
        .fax(merchant.getFax())
        .email(merchant.getEmail())
        .orders(orderDTOs)
        .grandTotal(grandTotal)
        .build();
  }

  // RPT-04: Invoice List (Single Merchant)

  /**
   * Generates an invoice list for a specific merchant within a date range.
   */
  public InvoiceListReportDTO generateMerchantInvoiceList(
      Integer merchantId, LocalDateTime from, LocalDateTime to) {

    List<Invoice> invoices =
        invoiceRepository.findByMerchant_MerchantIdAndInvoiceDateBetween(merchantId, from, to);

    List<InvoiceListReportDTO.InvoiceLineDTO> lines =
        invoices.stream().map(inv -> toInvoiceLineDTO(inv, false)).collect(Collectors.toList());

    return InvoiceListReportDTO.builder().from(from).to(to).invoices(lines).build();
  }

  // RPT-05: All Invoices

  /**
   * Generates an invoice list across all merchants within a date range.
   */
  public InvoiceListReportDTO generateAllInvoiceList(LocalDateTime from, LocalDateTime to) {

    List<Invoice> invoices = invoiceRepository.findByInvoiceDateBetween(from, to);

    List<InvoiceListReportDTO.InvoiceLineDTO> lines =
        invoices.stream().map(inv -> toInvoiceLineDTO(inv, true)).collect(Collectors.toList());

    return InvoiceListReportDTO.builder().from(from).to(to).invoices(lines).build();
  }

  // RPT-07: Low Stock Level Report

  /**
   * Generates a low stock report.
   */
  public LowStockReportDTO generateLowStockReport() {

    List<CatalogueItem> lowStock = catalogueItemRepository.findLowStockProducts();

    List<LowStockReportDTO.LowStockLineDTO> lines =
        lowStock.stream()
            .map(
                item -> {
                  // Recommended order = minStockLevel * (1 + buffer%) - current availability
                  BigDecimal target =
                      BigDecimal.valueOf(item.getMinStockLevel())
                          .multiply(
                              BigDecimal.ONE.add(
                                  item.getReorderBufferPct()
                                      .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

                  int recommendedQty = target.intValue() - item.getAvailability();
                  if (recommendedQty < 0) recommendedQty = 0;

                  return LowStockReportDTO.LowStockLineDTO.builder()
                      .productId(item.getProductId())
                      .description(item.getDescription())
                      .availability(item.getAvailability())
                      .minStockLevel(item.getMinStockLevel())
                      .recommendedOrderQty(recommendedQty)
                      .build();
                })
            .collect(Collectors.toList());

    return LowStockReportDTO.builder().generatedAt(LocalDateTime.now()).items(lines).build();
  }

  // RPT-06: Stock Turnover Report

  /**
   * Generates a stock turnover report for a given period.
   */
  public StockTurnoverReportDTO generateStockTurnoverReport(LocalDateTime from, LocalDateTime to) {

    // Reuse the existing product-sale-totals query for the period
    List<Object[]> rows = orderRepository.findProductSaleTotalsBetween(from, to);

    List<StockTurnoverReportDTO.StockTurnoverLineDTO> lines =
        rows.stream()
            .map(
                row -> {
                  String productId = (String) row[0];
                  String description = (String) row[1];
                  int qtySold = ((Number) row[2]).intValue();
                  BigDecimal revenue = (BigDecimal) row[3];

                  // Look up current availability
                  int currentAvail =
                      catalogueItemRepository
                          .findById(productId)
                          .map(item -> item.getAvailability())
                          .orElse(0);

                  return StockTurnoverReportDTO.StockTurnoverLineDTO.builder()
                      .productId(productId)
                      .description(description)
                      .quantitySold(qtySold)
                      .revenueSold(revenue)
                      .currentAvailability(currentAvail)
                      .build();
                })
            .collect(Collectors.toList());

    return StockTurnoverReportDTO.builder()
        .from(from)
        .to(to)
        .generatedAt(LocalDateTime.now())
        .lines(lines)
        .build();
  }

  // Invoice Line Mapper

  /**
   * Maps an Invoice entity to an InvoiceLineDTO for report output.
   */
  private InvoiceListReportDTO.InvoiceLineDTO toInvoiceLineDTO(
      Invoice inv, boolean includeMerchant) {
    BigDecimal totalPaid = paymentRepository.sumAmountPaidByInvoiceId(inv.getInvoiceId());

    return InvoiceListReportDTO.InvoiceLineDTO.builder()
        .invoiceId(inv.getInvoiceId())
        .orderId(inv.getOrder().getOrderId())
        .merchantId(includeMerchant ? inv.getMerchant().getMerchantId() : null)
        .merchantName(includeMerchant ? inv.getMerchant().getCompanyName() : null)
        .invoiceDate(inv.getInvoiceDate())
        .dueDate(inv.getDueDate())
        .amountDue(inv.getAmountDue())
        .totalPaid(totalPaid)
        .paymentStatus(inv.getPaymentStatus())
        .build();
  }

  // RPT-08: Debtor Reminders

  /**
   * Generates reminder records for all merchants with overdue invoices.
   */
  public DebtorReminderDTO generateDebtorReminders() {

    List<DebtorReminderDTO.ReminderLineDTO> reminders = new ArrayList<>();
    LocalDate today = LocalDate.now();

    // Find all merchants with at least one unpaid invoice
    List<Merchant> allMerchants = merchantRepository.findAll();

    for (Merchant merchant : allMerchants) {
      List<Invoice> unpaid =
          invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(merchant.getMerchantId());

      for (Invoice invoice : unpaid) {
        if (invoice.getDueDate() == null) continue;

        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
        if (daysOverdue <= 0) continue; // not yet overdue

        String reminderType;
        if (daysOverdue <= 15) {
          reminderType = "FIRST";
        } else {
          reminderType = "SECOND";
        }

        String reminderText = buildReminderText(merchant, invoice, reminderType, daysOverdue);

        reminders.add(
            DebtorReminderDTO.ReminderLineDTO.builder()
                .merchantId(merchant.getMerchantId())
                .companyName(merchant.getCompanyName())
                .address(merchant.getAddress())
                .phone(merchant.getPhone())
                .email(merchant.getEmail())
                .accountStatus(merchant.getAccountStatus().name())
                .invoiceId(invoice.getInvoiceId())
                .orderId(invoice.getOrder().getOrderId())
                .amountDue(invoice.getAmountDue())
                .dueDate(invoice.getDueDate())
                .daysOverdue(daysOverdue)
                .reminderType(reminderType)
                .reminderText(reminderText)
                .build());
      }
    }

    return DebtorReminderDTO.builder()
        .generatedAt(LocalDateTime.now())
        .reminders(reminders)
        .build();
  }

  /**
   * Builds the reminder letter text.
   */
  private String buildReminderText(
      Merchant merchant, Invoice invoice, String type, long daysOverdue) {

    String orderId = invoice.getOrder() != null ? invoice.getOrder().getOrderId() : "N/A";
    LocalDate paymentDeadline = LocalDate.now().plusDays(7);

    if ("FIRST".equals(type)) {
      return String.format(
          "Dear Client,\n\n"
              + "REMINDER - INVOICE NO.: %s\n"
              + "IPOS Account: %s    Total Amount: £%.2f\n\n"
              + "According to our records, it appears that we have not yet received payment "
              + "of the above invoice, which was raised against %s on %s, for ordering "
              + "pharmaceutical goods from InfoPharma Ltd, order %s.\n\n"
              + "We would appreciate payment by %s.\n\n"
              + "If you have already sent a payment to us recently, please accept our apologies.\n\n"
              + "Yours sincerely,\n"
              + "Director of Operations, InfoPharma Ltd.",
          invoice.getInvoiceId(),
          merchant.getMerchantId(),
          invoice.getAmountDue(),
          merchant.getCompanyName(),
          invoice.getInvoiceDate().toLocalDate(),
          orderId,
          paymentDeadline);
    } else {
      return String.format(
          "Dear Client,\n\n"
              + "SECOND REMINDER - INVOICE NO.: %s\n"
              + "IPOS Account: %s    Total Amount: £%.2f\n\n"
              + "It appears that we still have not yet received payment of the above invoice, "
              + "which was raised against %s on %s, for ordering pharmaceutical goods from "
              + "InfoPharma Ltd, order %s, despite the reminder sent to you previously.\n\n"
              + "We would appreciate it if you would settle this invoice in full by %s.\n\n"
              + "If you have already sent a payment to us recently, please accept our apologies.\n\n"
              + "Yours sincerely,\n"
              + "Director of Operations, InfoPharma Ltd.",
          invoice.getInvoiceId(),
          merchant.getMerchantId(),
          invoice.getAmountDue(),
          merchant.getCompanyName(),
          invoice.getInvoiceDate().toLocalDate(),
          orderId,
          paymentDeadline);
    }
  }
}
