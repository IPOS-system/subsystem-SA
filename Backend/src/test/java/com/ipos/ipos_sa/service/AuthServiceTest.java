package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.auth.LoginRequest;
import com.ipos.ipos_sa.dto.auth.LoginResponse;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.repository.InvoiceRepository;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.util.JwtUtil;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock UserRepository userRepository;
  @Mock MerchantRepository merchantRepository;
  @Mock InvoiceRepository invoiceRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtUtil jwtUtil;
  @Mock AccountService accountService;
  @InjectMocks AuthService authService;
  User staff;
  User merchUser;
  Merchant merchant;
  @BeforeEach
  void setup() {
    staff = User.builder().userId(1).username("sysdba")
        .passwordHash("HASHED").role(User.Role.ADMIN).isActive(true).build();

    merchUser = User.builder().userId(2).username("city")
        .passwordHash("HASHED").role(User.Role.MERCHANT).isActive(true).build();

    merchant = Merchant.builder()
        .merchantId(10).user(merchUser)
        .companyName("CityPharmacy")
        .accountStatus(Merchant.AccountStatus.NORMAL).build();}
  LoginRequest creds(String u, String p) {
    LoginRequest r = new LoginRequest();
    r.setUsername(u);
    r.setPassword(p);
    return r;}
  @Test
  void staffLogin_issuesJwtAndSkipsMerchantLookup() {
    when(userRepository.findByUsername("sysdba")).thenReturn(Optional.of(staff));
    when(passwordEncoder.matches("London_weighting", "HASHED")).thenReturn(true);
    when(jwtUtil.generateToken(1, "sysdba", User.Role.ADMIN)).thenReturn("JWT-TOKEN");
    LoginResponse res = authService.login(creds("sysdba", "London_weighting"));
    assertThat(res.getToken()).isEqualTo("JWT-TOKEN");
    assertThat(res.getRole()).isEqualTo(User.Role.ADMIN);
    assertThat(res.getMerchantId()).isNull();
    assertThat(res.isPaymentReminderDue()).isFalse();}
  @Test
  void merchantLogin_rechecksStatusAndReturnsMerchantId() {
    when(userRepository.findByUsername("city")).thenReturn(Optional.of(merchUser));
    when(passwordEncoder.matches("northampton", "HASHED")).thenReturn(true);
    when(merchantRepository.findByUser_UserId(2)).thenReturn(Optional.of(merchant));
    when(invoiceRepository.hasInvoicesDueForReminder(eq(10), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(false);
    when(jwtUtil.generateToken(2, "city", User.Role.MERCHANT)).thenReturn("JWT-M");

    LoginResponse res = authService.login(creds("city", "northampton"));

    assertThat(res.getMerchantId()).isEqualTo(10);
    assertThat(res.isPaymentReminderDue()).isFalse();
    verify(accountService).checkAndUpdateAccountStatus(10);}
  @Test
  void merchantLogin_overdueInvoice_raisesReminderFlag() {
    when(userRepository.findByUsername("city")).thenReturn(Optional.of(merchUser));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(merchantRepository.findByUser_UserId(2)).thenReturn(Optional.of(merchant));
    when(invoiceRepository.hasInvoicesDueForReminder(eq(10), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(true);
    when(jwtUtil.generateToken(2, "city", User.Role.MERCHANT)).thenReturn("JWT-M");

    assertThat(authService.login(creds("city", "northampton")).isPaymentReminderDue()).isTrue();}
  @Test
  void login_unknownUser_throws() {
    when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.login(creds("nobody", "x")))
        .isInstanceOf(IllegalArgumentException.class);}
  @Test
  void login_wrongPassword_throws() {
    when(userRepository.findByUsername("sysdba")).thenReturn(Optional.of(staff));
    when(passwordEncoder.matches("WRONG", "HASHED")).thenReturn(false);
    assertThatThrownBy(() -> authService.login(creds("sysdba", "WRONG")))
        .isInstanceOf(IllegalArgumentException.class);
    verify(jwtUtil, never()).generateToken(anyInt(), anyString(), any());}
  @Test
  void login_inactiveAccount_throws() {
    staff.setIsActive(false);
    when(userRepository.findByUsername("sysdba")).thenReturn(Optional.of(staff));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(creds("sysdba", "London_weighting")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("inactive");}}
