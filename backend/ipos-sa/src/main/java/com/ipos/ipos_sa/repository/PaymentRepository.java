package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /** Fetch all payments made against a specific invoice. */
    List<Payment> findByInvoice_InvoiceId(String invoiceId);

    /** Fetch all payments made by a merchant — used by detailed reports. */
    List<Payment> findByMerchant_MerchantId(Integer merchantId);

    /**
     * Sum of all payments recorded against a specific invoice.
     * Used by PaymentService to calculate the remaining outstanding balance
     * after each new payment is recorded.
     */
    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.invoice.invoiceId = :invoiceId")
    BigDecimal sumAmountPaidByInvoiceId(@Param("invoiceId") String invoiceId);
}