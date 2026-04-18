package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.DiscountTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountTierRepository extends JpaRepository<DiscountTier, Integer> {

  /**
   * Fetch tiers for a flexible plan sorted lowest-to-highest by minOrderVal.
   */
  List<DiscountTier> findByDiscountPlan_PlanIdOrderByMinOrderValAsc(Integer planId);
}
