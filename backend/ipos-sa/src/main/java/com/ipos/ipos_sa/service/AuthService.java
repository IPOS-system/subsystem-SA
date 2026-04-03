package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.auth.LoginRequest;
import com.ipos.ipos_sa.dto.auth.LoginResponse;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Authentication service responsible for login logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Authenticate user by username and password.
     *
     * @param request login request with username and password
     * @return LoginResponse with auth token and user details
     * @throws IllegalArgumentException if credentials are invalid
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
        LoginResponse response = LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole())
                .merchantId(null)
                .paymentReminderDue(false)
                .build();

        // For MERCHANT role, add merchant ID and check payment reminder
        if (user.getRole() == User.Role.MERCHANT) {
            Optional<Merchant> merchantOpt = merchantRepository.findByUser_UserId(user.getUserId());
            if (merchantOpt.isPresent()) {
                Merchant merchant = merchantOpt.get();
                response.setMerchantId(merchant.getMerchantId());
                // TODO: check for overdue invoices (1-15 days) and set paymentReminderDue
            }
        }

        // Attach token to response (or return separately depending on frontend expectations)
        response.setToken(token);

        return response;
    }
}
