package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.MonthlyDiscountTracker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MonthlyDiscountTrackerRepository extends JpaRepository<MonthlyDiscountTracker, Integer> {

    /**
     * Find the tracker record for a specific merchant and calendar month.
     * yearMonth format: "YYYY-MM" e.g. "2026-03".
     * Used to accumulate order values and calculate flexible discount at month-end.
     */
    Optional<MonthlyDiscountTracker> findByMerchant_MerchantIdAndYearMonth(
            Integer merchantId, String yearMonth);

    /** All unsettled flexible discount records — used by month-end settlement logic. */
    List<MonthlyDiscountTracker> findBySettledFalse();

    /** History of discount tracking for a merchant — used for reporting. */
    List<MonthlyDiscountTracker> findByMerchant_MerchantIdOrderByYearMonthDesc(Integer merchantId);
}