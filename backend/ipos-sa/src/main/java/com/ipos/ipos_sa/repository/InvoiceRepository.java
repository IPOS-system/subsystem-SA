package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
//import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /** My Balance screen: all invoices for a merchant. */
    List<Invoice> findByMerchant_MerchantId(Integer merchantId);

    /** Record Payment dropdown: only invoices that still have an outstanding balance. */
    List<Invoice> findByMerchant_MerchantIdAndPaymentStatusNot(
            Integer merchantId, Invoice.PaymentStatus status);

    /**
     * Account status check: find the oldest unpaid invoice for a merchant
     * so we can calculate how many days overdue the merchant is.
     * Used by checkAndUpdateAccountStatus() in AccountService.
     */
    @Query("SELECT i FROM Invoice i WHERE i.merchant.merchantId = :merchantId " +
           "AND i.paymentStatus <> 'PAID' " +
           "ORDER BY i.dueDate ASC")
    List<Invoice> findUnpaidByMerchantOrderByDueDateAsc(@Param("merchantId") Integer merchantId);

    /**
     * Payment reminder check: return true if the merchant has any invoice
     * that is overdue by 1–15 days (still NORMAL state, but reminder needed).
     */
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.merchant.merchantId = :merchantId " +
           "AND i.paymentStatus <> 'PAID' " +
           "AND i.dueDate < :today " +
           "AND i.dueDate >= :fifteenDaysAgo")
    boolean hasInvoicesDueForReminder(@Param("merchantId") Integer merchantId,
                                      @Param("today") LocalDate today,
                                      @Param("fifteenDaysAgo") LocalDate fifteenDaysAgo);

    /** RPT-04: invoice list for a merchant within a date range. */
    List<Invoice> findByMerchant_MerchantIdAndInvoiceDateBetween(
            Integer merchantId, LocalDateTime from, LocalDateTime to);

    /** RPT-05: all invoices across all merchants within a date range. */
    List<Invoice> findByInvoiceDateBetween(LocalDateTime from, LocalDateTime to);
}