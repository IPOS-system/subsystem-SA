package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.invoice.InvoiceDTO;
import com.ipos.ipos_sa.dto.payment.RecordPaymentRequest;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment recording.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

  private final PaymentService paymentService;
  private final UserRepository userRepository;

  /**
   * POST /api/payments Records a payment against a specific invoice.
   */
  @PostMapping
  public ResponseEntity<InvoiceDTO> recordPayment(
      @Valid @RequestBody RecordPaymentRequest request, Authentication auth) {

    requireRole(auth, User.Role.ACCOUNTANT, User.Role.ADMIN);
    User actingUser = resolveUser(auth);

    InvoiceDTO updated = paymentService.recordPayment(request, actingUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(updated);
  }

  // Helpers

  private User resolveUser(Authentication auth) {
    String username = auth.getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("Authenticated user not found: " + username));
  }

  private void requireRole(Authentication auth, User.Role... permitted) {
    String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    User.Role callerRole = User.Role.valueOf(roleStr);

    for (User.Role r : permitted) {
      if (r == callerRole) return;
    }
    throw new AccessDeniedException(
        "Role " + callerRole + " is not permitted to perform this action.");
  }
}
