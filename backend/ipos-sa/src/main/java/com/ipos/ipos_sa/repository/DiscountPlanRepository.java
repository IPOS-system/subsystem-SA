package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.DiscountPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountPlanRepository extends JpaRepository<DiscountPlan, Integer> {

  /** Filter plans by type — used when applying discount logic in OrderService. */
  List<DiscountPlan> findByPlanType(DiscountPlan.PlanType planType);

  /** Ordered list for dropdowns on account creation/edit screens. */
  List<DiscountPlan> findAllByOrderByPlanNameAsc();

  /**
   * Counts how many merchants are currently assigned this discount plan. Used to prevent deletion of
   * plans that are still in use.
   */
  @Query("SELECT COUNT(m) FROM Merchant m WHERE m.discountPlan.planId = :planId")
  long countMerchantsUsingPlan(@Param("planId") Integer planId);
}
