package com.ipos.ipos_sa.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service responsible for login logic.
 *
 * On merchant login, this service: 1. Validates credentials and active status. 2. Re-checks the
 * merchant's account status (15/30-day overdue rule). 3. Sets the paymentReminderDue flag if any
 * invoice is 1–15 days overdue. 4. Returns a JWT token with role-based claims.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final MerchantRepository merchantRepository;
  private final InvoiceRepository invoiceRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AccountService accountService;

  /**
   * Authenticate user by username and password.
   */
  public LoginResponse login(LoginRequest request) {
    // Find user by username
    Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
    if (userOpt.isEmpty()) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    User user = userOpt.get();

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    // Verify user is active
    if (!user.getIsActive()) {
      throw new IllegalArgumentException("User account is inactive");
    }

    // Generate JWT token
    String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

    // Build response
    LoginResponse response =
        LoginResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .role(user.getRole())
            .merchantId(null)
            .paymentReminderDue(false)
            .build();

    // For MERCHANT role, check account status and payment reminders
    if (user.getRole() == User.Role.MERCHANT) {
      Optional<Merchant> merchantOpt = merchantRepository.findByUser_UserId(user.getUserId());
      if (merchantOpt.isPresent()) {
        Merchant merchant = merchantOpt.get();
        response.setMerchantId(merchant.getMerchantId());

        // Re-check account status (15/30-day overdue rule)
        accountService.checkAndUpdateAccountStatus(merchant.getMerchantId());

        // Check if any invoice is 1–15 days overdue (reminder banner)
        response.setPaymentReminderDue(
            invoiceRepository.hasInvoicesDueForReminder(
                merchant.getMerchantId(), LocalDate.now(), LocalDate.now().minusDays(15)));
      }
    }

    // Attach token
    response.setToken(token);

    return response;
  }
}
