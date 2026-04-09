package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.DiscountTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountTierRepository extends JpaRepository<DiscountTier, Integer> {

  /**
   * Fetch tiers for a flexible plan sorted lowest-to-highest by minOrderVal. Used by OrderService /
   * MonthlyDiscountService when evaluating which tier applies to a merchant's monthly spend.
   */
  List<DiscountTier> findByDiscountPlan_PlanIdOrderByMinOrderValAsc(Integer planId);
}
