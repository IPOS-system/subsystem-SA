package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.discount.*;
import com.ipos.ipos_sa.entity.AuditLog;
import com.ipos.ipos_sa.entity.DiscountPlan;
import com.ipos.ipos_sa.entity.DiscountTier;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.AuditLogRepository;
import com.ipos.ipos_sa.repository.DiscountPlanRepository;
import com.ipos.ipos_sa.repository.DiscountTierRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountPlanService {

  private final DiscountPlanRepository discountPlanRepository;
  private final DiscountTierRepository discountTierRepository;
  private final AuditLogRepository auditLogRepository;

  // ── List All Plans ────────────────────────────────────────────────────────

  /**
   * Returns all discount plans ordered by name. Used to populate the dropdown on the merchant
   * account creation/edit screens.
   */
  public List<DiscountPlanDTO> getAllPlans() {
    return discountPlanRepository.findAllByOrderByPlanNameAsc().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // ── Get Single Plan ───────────────────────────────────────────────────────

  /** Returns a single discount plan by ID with full tier detail. */
  public DiscountPlanDTO getPlanById(Integer planId) {
    DiscountPlan plan =
        discountPlanRepository
            .findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("DiscountPlan", planId));
    return toDTO(plan);
  }

  // ── Create Fixed Plan ─────────────────────────────────────────────────────

  /**
   * Creates a fixed-rate discount plan where the same percentage applies to every order at the
   * point of sale.
   *
   * <p>Per the brief (IPOS-SA-ACC): "the same discount rate applies to all items of any quantity
   * ordered from the catalogue."
   */
  @Transactional
  public DiscountPlanDTO createFixedPlan(CreateFixedPlanRequest request, User actingUser) {

    DiscountPlan plan =
        DiscountPlan.builder()
            .planName(request.getPlanName())
            .planType(DiscountPlan.PlanType.FIXED)
            .fixedRate(request.getFixedRate())
            .build();

    discountPlanRepository.save(plan);

    audit(
        actingUser,
        "CREATE_DISCOUNT_PLAN",
        "discount_plan",
        String.valueOf(plan.getPlanId()),
        "Created FIXED plan: " + request.getPlanName() + " at " + request.getFixedRate() + "%");

    log.info(
        "Fixed discount plan created: {} ({}%)", request.getPlanName(), request.getFixedRate());
    return toDTO(plan);
  }

  // ── Create Flexible Plan ──────────────────────────────────────────────────

  /**
   * Creates a tiered (flexible) discount plan where the rate depends on total monthly spend.
   *
   * <p>Per the brief (IPOS-SA-ACC): "the discount rates depend on the value of all orders within a
   * calendar month, e.g. 1% for orders less than £1000, 2% for orders between £1000 and £2000 and
   * 3% for orders in excess of £2000."
   *
   * <p>The last tier should have maxOrderVal = null to represent no upper bound.
   */
  @Transactional
  public DiscountPlanDTO createFlexiblePlan(CreateFlexiblePlanRequest request, User actingUser) {

    DiscountPlan plan =
        DiscountPlan.builder()
            .planName(request.getPlanName())
            .planType(DiscountPlan.PlanType.FLEXIBLE)
            .fixedRate(BigDecimal.ZERO)
            .build();

    discountPlanRepository.save(plan);

    // Create each tier linked to this plan
    for (CreateFlexiblePlanRequest.TierRequest tierReq : request.getTiers()) {
      DiscountTier tier =
          DiscountTier.builder()
              .discountPlan(plan)
              .minOrderVal(tierReq.getMinOrderVal())
              .maxOrderVal(tierReq.getMaxOrderVal())
              .discountRate(tierReq.getDiscountRate())
              .build();
      discountTierRepository.save(tier);
    }

    audit(
        actingUser,
        "CREATE_DISCOUNT_PLAN",
        "discount_plan",
        String.valueOf(plan.getPlanId()),
        "Created FLEXIBLE plan: "
            + request.getPlanName()
            + " with "
            + request.getTiers().size()
            + " tiers");

    log.info(
        "Flexible discount plan created: {} ({} tiers)",
        request.getPlanName(),
        request.getTiers().size());

    // Re-fetch to include the saved tiers in the response
    return getPlanById(plan.getPlanId());
  }

  // ── Mapper ────────────────────────────────────────────────────────────────

  private DiscountPlanDTO toDTO(DiscountPlan plan) {

    List<DiscountPlanDTO.TierDTO> tierDTOs;

    if (plan.getPlanType() == DiscountPlan.PlanType.FLEXIBLE) {
      // Fetch tiers sorted by minOrderVal (lowest first)
      List<DiscountTier> tiers =
          discountTierRepository.findByDiscountPlan_PlanIdOrderByMinOrderValAsc(plan.getPlanId());

      tierDTOs =
          tiers.stream()
              .map(
                  t ->
                      DiscountPlanDTO.TierDTO.builder()
                          .tierId(t.getTierId())
                          .minOrderVal(t.getMinOrderVal())
                          .maxOrderVal(t.getMaxOrderVal())
                          .discountRate(t.getDiscountRate())
                          .build())
              .collect(Collectors.toList());
    } else {
      tierDTOs = Collections.emptyList();
    }

    return DiscountPlanDTO.builder()
        .planId(plan.getPlanId())
        .planName(plan.getPlanName())
        .planType(plan.getPlanType())
        .fixedRate(plan.getFixedRate())
        .tiers(tierDTOs)
        .build();
  }

  // ── Audit Helper ──────────────────────────────────────────────────────────

  private void audit(
      User actor, String action, String targetType, String targetId, String details) {
    AuditLog entry =
        AuditLog.builder()
            .user(actor)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .details(details)
            .build();
    auditLogRepository.save(entry);
  }
}
