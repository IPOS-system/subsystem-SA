package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.report.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository         orderRepository;
    private final OrderItemRepository     orderItemRepository;
    private final InvoiceRepository       invoiceRepository;
    private final MerchantRepository      merchantRepository;
    private final PaymentRepository       paymentRepository;
    private final CatalogueItemRepository catalogueItemRepository;

    // ── RPT-01: Turnover Report ───────────────────────────────────────────────

    /**
     * Generates a turnover report for a given date range (UC-22).
     *
     * Per the brief (Appendix 4 area / IPOS-SA-RPT item i):
     *   "Turnover for a given period of time in terms of quantities of goods
     *    sold to merchants and the revenue received by InfoPharma Ltd."
     *
     * Returns total revenue and a per-product breakdown of quantity sold
     * and revenue, excluding cancelled orders.
     */
    public TurnoverReportDTO generateTurnoverReport(LocalDateTime from, LocalDateTime to) {

        BigDecimal totalRevenue = orderRepository.sumRevenueBetween(from, to);

        List<Object[]> rows = orderRepository.findProductSaleTotalsBetween(from, to);

        List<TurnoverReportDTO.ProductTurnoverDTO> lines = rows.stream()
                .map(row -> TurnoverReportDTO.ProductTurnoverDTO.builder()
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

    // ── RPT-02: Merchant Orders Summary ───────────────────────────────────────

    /**
     * Generates a merchant orders summary report (UC-23 / Appendix 4).
     *
     * Per the brief: "List of orders received from a particular merchant for
     * a given period of time with: order ID, date of ordering, value of the
     * order, date of dispatching, payment received (or pending), a 'Totals'
     * line should conclude the report."
     */
    public MerchantOrdersSummaryDTO generateMerchantOrdersSummary(
            Integer merchantId, LocalDateTime from, LocalDateTime to) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        List<Order> orders = orderRepository
                .findByMerchant_MerchantIdAndOrderDateBetween(merchantId, from, to);

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

            lines.add(MerchantOrdersSummaryDTO.OrderSummaryLineDTO.builder()
                    .orderId(order.getOrderId())
                    .orderedDate(order.getOrderDate())
                    .amount(order.getTotalAmount())
                    .dispatchedDate(order.getDispatchDate())
                    .deliveredDate(order.getStatus() == Order.OrderStatus.DELIVERED
                            ? order.getDispatchDate() : null) // best available proxy
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

    // ── RPT-03: Merchant Detailed Report ──────────────────────────────────────

    /**
     * Generates a detailed merchant report (Appendix 5).
     *
     * Per the brief: "contact details of a merchant (as a header), list of
     * all orders (including the individual items ordered, quantity ordered,
     * individual cost, total cost of order, discount given if known,
     * payment status)."
     */
    public MerchantDetailedReportDTO generateMerchantDetailedReport(
            Integer merchantId, LocalDateTime from, LocalDateTime to) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        List<Order> orders = orderRepository
                .findByMerchant_MerchantIdAndOrderDateBetween(merchantId, from, to);

        BigDecimal grandTotal = BigDecimal.ZERO;

        List<MerchantDetailedReportDTO.DetailedOrderDTO> orderDTOs = new java.util.ArrayList<>();

        for (Order order : orders) {
            // Fetch line items for this order
            List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());

            List<MerchantDetailedReportDTO.LineItemDTO> itemDTOs = items.stream()
                    .map(item -> MerchantDetailedReportDTO.LineItemDTO.builder()
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

            orderDTOs.add(MerchantDetailedReportDTO.DetailedOrderDTO.builder()
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

    // ── RPT-04: Invoice List (Single Merchant) ────────────────────────────────

    /**
     * Generates an invoice list for a specific merchant within a date range.
     * Per the brief: "List of invoices raised against an individual merchant
     * for a given period."
     */
    public InvoiceListReportDTO generateMerchantInvoiceList(
            Integer merchantId, LocalDateTime from, LocalDateTime to) {

        List<Invoice> invoices = invoiceRepository
                .findByMerchant_MerchantIdAndInvoiceDateBetween(merchantId, from, to);

        List<InvoiceListReportDTO.InvoiceLineDTO> lines = invoices.stream()
                .map(inv -> toInvoiceLineDTO(inv, false))
                .collect(Collectors.toList());

        return InvoiceListReportDTO.builder()
                .from(from)
                .to(to)
                .invoices(lines)
                .build();
    }

    // ── RPT-05: All Invoices ──────────────────────────────────────────────────

    /**
     * Generates an invoice list across all merchants within a date range.
     * Per the brief: "List of all invoices raised by InfoPharma Ltd against
     * merchants for a given period."
     */
    public InvoiceListReportDTO generateAllInvoiceList(
            LocalDateTime from, LocalDateTime to) {

        List<Invoice> invoices = invoiceRepository.findByInvoiceDateBetween(from, to);

        List<InvoiceListReportDTO.InvoiceLineDTO> lines = invoices.stream()
                .map(inv -> toInvoiceLineDTO(inv, true))
                .collect(Collectors.toList());

        return InvoiceListReportDTO.builder()
                .from(from)
                .to(to)
                .invoices(lines)
                .build();
    }

    // ── RPT-07: Low Stock Level Report ────────────────────────────────────────

    /**
     * Generates a low stock report (UC-46 / Appendix 3).
     *
     * Per the brief: all active products below their minimum stock level,
     * with a recommended minimum order quantity calculated as:
     *   minStockLevel * (1 + reorderBufferPct / 100) - availability
     *
     * This brings stock up to the minimum level plus the buffer.
     */
    public LowStockReportDTO generateLowStockReport() {

        List<CatalogueItem> lowStock = catalogueItemRepository.findLowStockProducts();

        List<LowStockReportDTO.LowStockLineDTO> lines = lowStock.stream()
                .map(item -> {
                    // Recommended order = minStockLevel * (1 + buffer%) - current availability
                    BigDecimal target = BigDecimal.valueOf(item.getMinStockLevel())
                            .multiply(BigDecimal.ONE.add(
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

        return LowStockReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .items(lines)
                .build();
    }

    // ── Invoice Line Mapper ───────────────────────────────────────────────────

    /**
     * Maps an Invoice entity to an InvoiceLineDTO for report output.
     *
     * @param inv             the invoice entity
     * @param includeMerchant if true, populates merchantId and merchantName
     *                        (used by RPT-05 all-invoices; null for RPT-04)
     */
    private InvoiceListReportDTO.InvoiceLineDTO toInvoiceLineDTO(Invoice inv,
                                                                   boolean includeMerchant) {
        BigDecimal totalPaid = paymentRepository
                .sumAmountPaidByInvoiceId(inv.getInvoiceId());

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
}