package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.account.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.*;
import com.ipos.ipos_sa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock UserRepository userRepository;
  @Mock MerchantRepository merchantRepository;
  @Mock DiscountPlanRepository discountPlanRepository;
  @Mock InvoiceRepository invoiceRepository;
  @Mock AuditLogRepository auditLogRepository;
  @Mock PasswordEncoder passwordEncoder;

  @InjectMocks AccountService accountService;

  User admin;
  DiscountPlan plan;

  @BeforeEach
  void setup() {
    admin = User.builder().userId(1).username("sysdba").role(User.Role.ADMIN).isActive(true).build();
    plan = DiscountPlan.builder()
        .planId(10).planName("Fixed 3%")
        .planType(DiscountPlan.PlanType.FIXED)
        .fixedRate(new BigDecimal("3.00"))
        .build();}
  Merchant merchant(Merchant.AccountStatus status) {
    return Merchant.builder()
        .merchantId(1)
        .user(User.builder().userId(2).username("city").isActive(true).build())
        .companyName("CityPharmacy")
        .creditLimit(new BigDecimal("10000"))
        .currentBalance(new BigDecimal("500"))
        .accountStatus(status)
        .discountPlan(plan)
        .build();}
  Invoice unpaid(LocalDate due) {
    return Invoice.builder()
        .invoiceId("INV-1").dueDate(due)
        .amountDue(new BigDecimal("500"))
        .paymentStatus(Invoice.PaymentStatus.PENDING)
        .build();}
  CreateMerchantRequest merchReq() {
    CreateMerchantRequest r = new CreateMerchantRequest();
    r.setUsername("city");
    r.setPassword("northampton");
    r.setCompanyName("CityPharmacy");
    r.setAddress("Northampton Square");
    r.setPhone("0207 040 8000");
    r.setEmail("city@example.com");
    r.setCreditLimit(new BigDecimal("10000"));
    r.setDiscountPlanId(10);
    return r;}
  @Test
  void createMerchant_valid_savesUserAndMerchant() {
    when(userRepository.existsByUsername("city")).thenReturn(false);
    when(discountPlanRepository.findById(10)).thenReturn(Optional.of(plan));
    when(passwordEncoder.encode("northampton")).thenReturn("HASHED");
    when(userRepository.save(any(User.class))).thenAnswer(i -> {
      User u = i.getArgument(0); u.setUserId(42); return u;});
    when(merchantRepository.save(any(Merchant.class))).thenAnswer(i -> {
      Merchant m = i.getArgument(0); m.setMerchantId(100); return m;});
    MerchantDTO result = accountService.createMerchantAccount(merchReq(), admin);

    
    
    
    
    assertThat(result.getCompanyName()).isEqualTo("CityPharmacy");
    assertThat(result.getCreditLimit()).isEqualByComparingTo("10000");
    assertThat(result.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);

    ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCap.capture());
    
    
    
    
    
    assertThat(userCap.getValue().getRole()).isEqualTo(User.Role.MERCHANT);
    assertThat(userCap.getValue().getPasswordHash()).isEqualTo("HASHED");
    verify(auditLogRepository).save(any());}
  @Test
  void createMerchant_duplicateUsername_throws() {
    when(userRepository.existsByUsername("city")).thenReturn(true);
    assertThatThrownBy(() -> accountService.createMerchantAccount(merchReq(), admin))
        .isInstanceOf(DuplicateUsernameException.class);
    verify(userRepository, never()).save(any());}
  @Test
  void createMerchant_unknownPlan_throws() {
    when(userRepository.existsByUsername("city")).thenReturn(false);
    when(discountPlanRepository.findById(10)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> accountService.createMerchantAccount(merchReq(), admin))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void createStaff_valid_createsManager() {
    CreateStaffRequest r = new CreateStaffRequest();
    r.setUsername("manager"); r.setPassword("Get_it_done"); r.setRole(User.Role.MANAGER);
    when(userRepository.existsByUsername("manager")).thenReturn(false);
    when(passwordEncoder.encode("Get_it_done")).thenReturn("HASHED");
    when(userRepository.save(any(User.class))).thenAnswer(i -> {
      
      
      
      
      
      User u = i.getArgument(0); u.setUserId(7); return u;});
    StaffDTO result = accountService.createStaffAccount(r, admin);
    assertThat(result.getRole()).isEqualTo(User.Role.MANAGER);
    assertThat(result.getIsActive()).isTrue();}
  @Test
  void createStaff_merchantRole_rejected() {
    CreateStaffRequest r = new CreateStaffRequest();
    r.setUsername("x"); r.setRole(User.Role.MERCHANT);
    assertThatThrownBy(() -> accountService.createStaffAccount(r, admin))
        .isInstanceOf(ValidationException.class);
    verify(userRepository, never()).save(any());}

  @Test
  void createStaff_duplicate_throws() {
    CreateStaffRequest r = new CreateStaffRequest();
    r.setUsername("sysdba"); r.setRole(User.Role.ADMIN);
    when(userRepository.existsByUsername("sysdba")).thenReturn(true);
    assertThatThrownBy(() -> accountService.createStaffAccount(r, admin))
        .isInstanceOf(DuplicateUsernameException.class);}

  
  
  
  
  @Test
  void deactivate_setsInactive() {
    User u = User.builder().userId(5).username("clerk").isActive(true).build();
    when(userRepository.findById(5)).thenReturn(Optional.of(u));
    accountService.deactivateAccount(5, admin);
    assertThat(u.getIsActive()).isFalse();
    verify(userRepository).save(u);
  }

  @Test
  void reactivate_setsActive() {
    User u = User.builder().userId(5).username("clerk").isActive(false).build();
    when(userRepository.findById(5)).thenReturn(Optional.of(u));
    accountService.reactivateAccount(5, admin);
    assertThat(u.getIsActive()).isTrue();
  }

  @Test
  void deactivate_unknownUser_throws() {
    when(userRepository.findById(99)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> accountService.deactivateAccount(99, admin))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void changeRole_promote() {
    User u = User.builder().userId(3).username("clerk").role(User.Role.ACCOUNTANT).isActive(true).build();
    when(userRepository.findById(3)).thenReturn(Optional.of(u));
    StaffDTO result = accountService.changeUserRole(3, User.Role.MANAGER, admin);
    assertThat(result.getRole()).isEqualTo(User.Role.MANAGER);
    assertThat(u.getRole()).isEqualTo(User.Role.MANAGER);}
  @Test
  void changeRole_demote() {
    User u = User.builder().userId(3).username("manager").role(User.Role.MANAGER).isActive(true).build();
    when(userRepository.findById(3)).thenReturn(Optional.of(u));
    StaffDTO result = accountService.changeUserRole(3, User.Role.ACCOUNTANT, admin);
    assertThat(result.getRole()).isEqualTo(User.Role.ACCOUNTANT);}
  @Test
  void updateMerchant_partial_onlyTouchesGivenFields() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    UpdateMerchantRequest r = new UpdateMerchantRequest();
    r.setEmail("new@example.com");
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    MerchantDTO result = accountService.updateMerchantDetails(1, r, admin);

    
    
    
    
    assertThat(result.getEmail()).isEqualTo("new@example.com");
    assertThat(result.getCompanyName()).isEqualTo("CityPharmacy");
    assertThat(result.getCreditLimit()).isEqualByComparingTo("10000");
  }

  @Test
  void updateMerchant_changeCreditLimit() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    UpdateMerchantRequest r = new UpdateMerchantRequest();
    r.setCreditLimit(new BigDecimal("15000"));

    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    assertThat(accountService.updateMerchantDetails(1, r, admin).getCreditLimit())
        .isEqualByComparingTo("15000");
  }

  @Test
  void updateMerchant_changeDiscountPlan() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    DiscountPlan newPlan = DiscountPlan.builder()
        .planId(20).planName("Flexible")
        .planType(DiscountPlan.PlanType.FLEXIBLE).fixedRate(BigDecimal.ZERO).build();
    UpdateMerchantRequest r = new UpdateMerchantRequest();
    r.setDiscountPlanId(20);

    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(discountPlanRepository.findById(20)).thenReturn(Optional.of(newPlan));

    MerchantDTO result = accountService.updateMerchantDetails(1, r, admin);
    assertThat(result.getDiscountPlanId()).isEqualTo(20);
    assertThat(result.getDiscountPlanName()).isEqualTo("Flexible");
  }

  @Test
  void updateMerchant_unknownPlan_throws() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    UpdateMerchantRequest r = new UpdateMerchantRequest();
    r.setDiscountPlanId(999);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(discountPlanRepository.findById(999)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.updateMerchantDetails(1, r, admin))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void restoreFromDefault_valid_restoresAndRecordsAuthoriser() {
    User manager = User.builder().userId(2).username("manager").role(User.Role.MANAGER).isActive(true).build();
    Merchant m = merchant(Merchant.AccountStatus.IN_DEFAULT);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));

    MerchantDTO result = accountService.restoreFromDefault(1, manager);

    assertThat(result.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);
    assertThat(m.getAuthorizedBy()).isEqualTo(manager);}
  @Test
  void restoreFromDefault_notInDefault_rejected() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    assertThatThrownBy(() -> accountService.restoreFromDefault(1, admin))
        .isInstanceOf(ValidationException.class);}
  @Test
  void statusCheck_inDefault_untouched() {
    Merchant m = merchant(Merchant.AccountStatus.IN_DEFAULT);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    accountService.checkAndUpdateAccountStatus(1);
    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.IN_DEFAULT);
    verify(merchantRepository, never()).save(any());}
  @Test
  void statusCheck_noUnpaid_restoresNormal() {
    Merchant m = merchant(Merchant.AccountStatus.SUSPENDED);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1)).thenReturn(Collections.emptyList());

    accountService.checkAndUpdateAccountStatus(1);

    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);
    verify(merchantRepository).save(m);}
  @Test
  void statusCheck_freshOverdue_staysNormal() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1))
        .thenReturn(List.of(unpaid(LocalDate.now().minusDays(5))));

    accountService.checkAndUpdateAccountStatus(1);

    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);
    verify(merchantRepository, never()).save(any());
  }
  @Test
  void statusCheck_20daysOverdue_suspends() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1))
            
        .thenReturn(List.of(unpaid(LocalDate.now().minusDays(20))));

    accountService.checkAndUpdateAccountStatus(1);

    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.SUSPENDED);
  }

  @Test
  void statusCheck_45daysOverdue_inDefault() {
    Merchant m = merchant(Merchant.AccountStatus.SUSPENDED);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1))
        .thenReturn(List.of(unpaid(LocalDate.now().minusDays(45))));

    accountService.checkAndUpdateAccountStatus(1);

    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.IN_DEFAULT);
  }

  @Test
  void statusCheck_exactly15days_stillNormal() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1))
        .thenReturn(List.of(unpaid(LocalDate.now().minusDays(15))));
    

    accountService.checkAndUpdateAccountStatus(1);

    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.NORMAL);
  }

  @Test
  void statusCheck_exactly30days_suspended() {
    Merchant m = merchant(Merchant.AccountStatus.NORMAL);
    when(merchantRepository.findById(1)).thenReturn(Optional.of(m));
    when(invoiceRepository.findUnpaidByMerchantOrderByDueDateAsc(1))
        .thenReturn(List.of(unpaid(LocalDate.now().minusDays(30))));
    accountService.checkAndUpdateAccountStatus(1);
    assertThat(m.getAccountStatus()).isEqualTo(Merchant.AccountStatus.SUSPENDED);}
  @Test
  void resetPassword_hashesAndSaves() {
    User u = User.builder().userId(5).username("clerk").passwordHash("OLD").isActive(true).build();
    when(userRepository.findById(5)).thenReturn(Optional.of(u));
    when(passwordEncoder.encode("fresh_pw")).thenReturn("NEW_HASH");

    
    
    accountService.resetPassword(5, "fresh_pw", admin);
    assertThat(u.getPasswordHash()).isEqualTo("NEW_HASH");
    verify(userRepository).save(u);}
  @Test
  void searchMerchants_delegates() {
    Merchant m = Merchant.builder()
        .merchantId(1)
        .user(User.builder().userId(2).username("city").isActive(true).build())
        .companyName("CityPharmacy")
        .creditLimit(BigDecimal.TEN).currentBalance(BigDecimal.ZERO)
        .accountStatus(Merchant.AccountStatus.NORMAL)
        .statusChangedAt(LocalDateTime.now())
        .build();
    when(merchantRepository.searchByNameOrEmail("city")).thenReturn(List.of(m));
    List<MerchantDTO> result = accountService.searchMerchants("city");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCompanyName()).isEqualTo("CityPharmacy");}
  @Test
  void getMerchantById_missing_throws() {
    when(merchantRepository.findById(999)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> accountService.getMerchantById(999))
        .isInstanceOf(ResourceNotFoundException.class);}}
