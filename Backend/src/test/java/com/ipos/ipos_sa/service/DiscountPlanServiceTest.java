package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.discount.*;
import com.ipos.ipos_sa.entity.DiscountPlan;
import com.ipos.ipos_sa.entity.DiscountTier;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.AuditLogRepository;
import com.ipos.ipos_sa.repository.DiscountPlanRepository;
import com.ipos.ipos_sa.repository.DiscountTierRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscountPlanServiceTest {

  @Mock DiscountPlanRepository discountPlanRepository;
  @Mock DiscountTierRepository discountTierRepository;
  @Mock AuditLogRepository auditLogRepository;

  @InjectMocks DiscountPlanService discountPlanService;

  User admin;

  @BeforeEach
  void setup() {
    admin = User.builder().userId(1).username("sysdba").role(User.Role.ADMIN).isActive(true).build();}
  @Test
  void createFixedPlan_persistsWithGivenRate() {
    CreateFixedPlanRequest r = new CreateFixedPlanRequest();
    r.setPlanName("Fixed 3%");
    r.setFixedRate(new BigDecimal("3.00"));

    when(discountPlanRepository.save(any(DiscountPlan.class))).thenAnswer(i -> {
      DiscountPlan p = i.getArgument(0); p.setPlanId(10); return p;});

    
    
    DiscountPlanDTO result = discountPlanService.createFixedPlan(r, admin);

    
    
    assertThat(result.getPlanId()).isEqualTo(10);
    assertThat(result.getPlanType()).isEqualTo(DiscountPlan.PlanType.FIXED);
    assertThat(result.getFixedRate()).isEqualByComparingTo("3.00");
    assertThat(result.getTiers()).isEmpty();
  }

  @Test
  void createFlexiblePlan_persistsAllTiers() {
    CreateFlexiblePlanRequest r = new CreateFlexiblePlanRequest();
    r.setPlanName("Tiered (Cosymed)");
    r.setTiers(List.of(
        tier(new BigDecimal("0"), new BigDecimal("1000"), new BigDecimal("0")),
        tier(new BigDecimal("1000"), new BigDecimal("2000"), new BigDecimal("1")),
        tier(new BigDecimal("2000"), null, new BigDecimal("2"))
    ));

    when(discountPlanRepository.save(any(DiscountPlan.class))).thenAnswer(i -> {
      DiscountPlan p = i.getArgument(0); p.setPlanId(20); return p;
    });
    DiscountPlan saved = DiscountPlan.builder()
        .planId(20).planName("Tiered (Cosymed)")
        .planType(DiscountPlan.PlanType.FLEXIBLE)
        .fixedRate(BigDecimal.ZERO).build();
    when(discountPlanRepository.findById(20)).thenReturn(Optional.of(saved));
    when(discountTierRepository.findByDiscountPlan_PlanIdOrderByMinOrderValAsc(20))
        .thenReturn(List.of(
            DiscountTier.builder().tierId(1).minOrderVal(new BigDecimal("0"))
                .maxOrderVal(new BigDecimal("1000")).discountRate(new BigDecimal("0")).build(),
            DiscountTier.builder().tierId(2).minOrderVal(new BigDecimal("1000"))
                .maxOrderVal(new BigDecimal("2000")).discountRate(new BigDecimal("1")).build(),
            
            
            
            DiscountTier.builder().tierId(3).minOrderVal(new BigDecimal("2000"))
                .maxOrderVal(null).discountRate(new BigDecimal("2")).build()));

    DiscountPlanDTO result = discountPlanService.createFlexiblePlan(r, admin);
    assertThat(result.getPlanType()).isEqualTo(DiscountPlan.PlanType.FLEXIBLE);
    assertThat(result.getTiers()).hasSize(3);
    assertThat(result.getTiers().get(2).getMaxOrderVal()).isNull();
    verify(discountTierRepository, times(3)).save(any(DiscountTier.class));}
  CreateFlexiblePlanRequest.TierRequest tier(BigDecimal min, BigDecimal max, BigDecimal rate) {
    CreateFlexiblePlanRequest.TierRequest t = new CreateFlexiblePlanRequest.TierRequest();
    t.setMinOrderVal(min);
    t.setMaxOrderVal(max);
    t.setDiscountRate(rate);
    return t;}
  @Test
  void deletePlan_unused_deletes() {
    DiscountPlan p = DiscountPlan.builder()
        .planId(10).planName("Fixed 3%")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("3")).build();

    when(discountPlanRepository.findById(10)).thenReturn(Optional.of(p));
    
    when(discountPlanRepository.countMerchantsUsingPlan(10)).thenReturn(0L);

    discountPlanService.deletePlan(10, admin);

    verify(discountPlanRepository).delete(p);}
  @Test
  void deletePlan_inUse_rejected() {
    DiscountPlan p = DiscountPlan.builder()
        .planId(10).planName("Fixed 3%")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("3")).build();

    when(discountPlanRepository.findById(10)).thenReturn(Optional.of(p));
    when(discountPlanRepository.countMerchantsUsingPlan(10)).thenReturn(2L);
    assertThatThrownBy(() -> discountPlanService.deletePlan(10, admin))
        .isInstanceOf(ValidationException.class);
    verify(discountPlanRepository, never()).delete(any());}
  @Test
  void deletePlan_unknown_throws() {
    when(discountPlanRepository.findById(999)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> discountPlanService.deletePlan(999, admin))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void getAllPlans_orderedByName() {
    DiscountPlan p = DiscountPlan.builder()
        .planId(1).planName("A-Plan")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("5")).build();
    when(discountPlanRepository.findAllByOrderByPlanNameAsc()).thenReturn(List.of(p));

    List<DiscountPlanDTO> result = discountPlanService.getAllPlans();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlanName()).isEqualTo("A-Plan");
  }
  @Test
  void getPlanById_fixed_noTiers() {
    DiscountPlan p = DiscountPlan.builder()
        .planId(5).planName("Fixed 10%")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("10")).build();
    when(discountPlanRepository.findById(5)).thenReturn(Optional.of(p));

    DiscountPlanDTO result = discountPlanService.getPlanById(5);

    assertThat(result.getTiers()).isEmpty();
    assertThat(result.getFixedRate()).isEqualByComparingTo("10");}
  @Test
  void getPlanById_missing_throws() {
    when(discountPlanRepository.findById(999)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> discountPlanService.getPlanById(999))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void getPlanById_flexible_loadsTiers() {
    DiscountPlan p = DiscountPlan.builder()
        .planId(20).planName("Tiered")
        .planType(DiscountPlan.PlanType.FLEXIBLE)
        .fixedRate(BigDecimal.ZERO).build();
    when(discountPlanRepository.findById(20)).thenReturn(Optional.of(p));
    when(discountTierRepository.findByDiscountPlan_PlanIdOrderByMinOrderValAsc(20))
        .thenReturn(Collections.emptyList());
    DiscountPlanDTO result = discountPlanService.getPlanById(20);

    assertThat(result.getPlanType()).isEqualTo(DiscountPlan.PlanType.FLEXIBLE);
    assertThat(result.getTiers()).isEmpty();}}
