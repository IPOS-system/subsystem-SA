package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.report.*;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.service.ReportService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for report generation (IPOS-SA-RPT / UC-31).
 *
 * <p>All reports are MANAGER-only unless otherwise noted. ADMIN also has access for operational
 * convenience.
 *
 * <p>Date parameters use ISO format: 2026-01-01T00:00:00
 *
 * <p>Endpoints: GET /api/reports/turnover?from=&to= → RPT-01 GET
 * /api/reports/merchant-summary?merchantId=&from=&to= → RPT-02 GET
 * /api/reports/merchant-detailed?merchantId=&from=&to= → RPT-03 GET
 * /api/reports/invoice-list?merchantId=&from=&to= → RPT-04 GET /api/reports/all-invoices?from=&to=
 * → RPT-05 GET /api/reports/low-stock → RPT-07
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

  private final ReportService reportService;

  // ── RPT-01: Turnover Report ───────────────────────────────────────────────

  /**
   * GET /api/reports/turnover?from=...&to=... Quantity sold and revenue received per product within
   * a date range.
   */
  @GetMapping("/turnover")
  public ResponseEntity<TurnoverReportDTO> turnoverReport(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN);
    return ResponseEntity.ok(reportService.generateTurnoverReport(from, to));
  }

  // ── RPT-02: Merchant Orders Summary ───────────────────────────────────────

  /**
   * GET /api/reports/merchant-summary?merchantId=...&from=...&to=... Order list for a specific
   * merchant with totals row (Appendix 4).
   */
  @GetMapping("/merchant-summary")
  public ResponseEntity<MerchantOrdersSummaryDTO> merchantOrdersSummary(
      @RequestParam Integer merchantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN);
    return ResponseEntity.ok(reportService.generateMerchantOrdersSummary(merchantId, from, to));
  }

  // ── RPT-03: Merchant Detailed Report ──────────────────────────────────────

  /**
   * GET /api/reports/merchant-detailed?merchantId=...&from=...&to=... Contact details as header,
   * all orders with line item breakdown (Appendix 5).
   */
  @GetMapping("/merchant-detailed")
  public ResponseEntity<MerchantDetailedReportDTO> merchantDetailedReport(
      @RequestParam Integer merchantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN);
    return ResponseEntity.ok(reportService.generateMerchantDetailedReport(merchantId, from, to));
  }

  // ── RPT-04: Invoice List (Single Merchant) ────────────────────────────────

  /**
   * GET /api/reports/invoice-list?merchantId=...&from=...&to=... All invoices for a specific
   * merchant within a date range.
   */
  @GetMapping("/invoice-list")
  public ResponseEntity<InvoiceListReportDTO> merchantInvoiceList(
      @RequestParam Integer merchantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN, User.Role.ACCOUNTANT);
    return ResponseEntity.ok(reportService.generateMerchantInvoiceList(merchantId, from, to));
  }

  // ── RPT-05: All Invoices ──────────────────────────────────────────────────

  /**
   * GET /api/reports/all-invoices?from=...&to=... All invoices across all merchants within a date
   * range.
   */
  @GetMapping("/all-invoices")
  public ResponseEntity<InvoiceListReportDTO> allInvoiceList(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN, User.Role.ACCOUNTANT);
    return ResponseEntity.ok(reportService.generateAllInvoiceList(from, to));
  }

  // ── RPT-07: Low Stock Level Report ────────────────────────────────────────

  /**
   * GET /api/reports/low-stock All active products below minimum stock level with recommended order
   * quantities (Appendix 3 / UC-46).
   */
  @GetMapping("/low-stock")
  public ResponseEntity<LowStockReportDTO> lowStockReport(Authentication auth) {
    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN);
    return ResponseEntity.ok(reportService.generateLowStockReport());
  }

  // ── RPT-08: Debtor Reminders ──────────────────────────────────────────────

  /**
   * GET /api/reports/reminders Generates reminder records for all merchants with overdue invoices.
   *
   * <p>Per the marking sheet (4 marks): "Generating reminders to merchant debtors (shown on CA
   * screen) every time a merchant debtor tries to log in."
   *
   * <p>Returns FIRST reminders (1–15 days overdue) and SECOND reminders (15+ days overdue), each
   * with pre-formatted text matching Appendix 6 layout.
   *
   * <p>ADMIN, MANAGER, and ACCOUNTANT roles can generate reminders.
   */
  @GetMapping("/reminders")
  public ResponseEntity<DebtorReminderDTO> debtorReminders(Authentication auth) {
    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN, User.Role.ACCOUNTANT);
    return ResponseEntity.ok(reportService.generateDebtorReminders());
  }

  // ── RPT-06: Stock Turnover Report ─────────────────────────────────────────

  /**
   * GET /api/reports/stock-turnover?from=...&to=... Per the brief (IPOS-SA-RPT item vi): "Stock
   * turnover (goods sold and newly received) within a given period of time."
   */
  @GetMapping("/stock-turnover")
  public ResponseEntity<StockTurnoverReportDTO> stockTurnoverReport(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.MANAGER, User.Role.ADMIN);
    return ResponseEntity.ok(reportService.generateStockTurnoverReport(from, to));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void requireRole(Authentication auth, User.Role... permitted) {
    String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    User.Role callerRole = User.Role.valueOf(roleStr);

    for (User.Role r : permitted) {
      if (r == callerRole) return;
    }
    throw new AccessDeniedException(
        "Role " + callerRole + " is not permitted to perform this action.");
  }
}
