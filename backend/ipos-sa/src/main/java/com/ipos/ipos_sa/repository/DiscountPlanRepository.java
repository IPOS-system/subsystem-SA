package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.DiscountPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountPlanRepository extends JpaRepository<DiscountPlan, Integer> {

  /** Filter plans by type — used when applying discount logic in OrderService. */
  List<DiscountPlan> findByPlanType(DiscountPlan.PlanType planType);

  /** Ordered list for dropdowns on account creation/edit screens. */
  List<DiscountPlan> findAllByOrderByPlanNameAsc();
}
