package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.MonthlyDiscountTracker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyDiscountTrackerRepository
    extends JpaRepository<MonthlyDiscountTracker, Integer> {

  /**
   * Find the tracker record for a specific merchant and calendar month.
   */
  Optional<MonthlyDiscountTracker> findByMerchant_MerchantIdAndYearMonth(
      Integer merchantId, String yearMonth);

  /** All unsettled flexible discount records. */
  List<MonthlyDiscountTracker> findBySettledFalse();

  /** History of discount tracking for a merchant */
  List<MonthlyDiscountTracker> findByMerchant_MerchantIdOrderByYearMonthDesc(Integer merchantId);
}
