package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.invoice.InvoiceDTO;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for invoice viewing (UC-18, UC-19).
 *
 * Role rules:
 *   GET /api/invoices/{id}                    → any authenticated user (own invoice for merchants)
 *   GET /api/invoices/merchant/{merchantId}   → MERCHANT (own), ACCOUNTANT, ADMIN, MANAGER
 *   GET /api/invoices/merchant/{merchantId}/unpaid → ACCOUNTANT, ADMIN (record payment dropdown)
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final PaymentService    paymentService;
    private final UserRepository    userRepository;
    private final MerchantRepository merchantRepository;

    // ── View Single Invoice (UC-18) ───────────────────────────────────────────

    /**
     * GET /api/invoices/{id}
     * Returns a single invoice with full detail including payment history.
     * Merchants can only view their own invoices.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoice(
            @PathVariable String id,
            Authentication auth) {

        InvoiceDTO invoice = paymentService.getInvoiceById(id);

        // Merchants can only view their own invoices
        if (hasRole(auth, User.Role.MERCHANT)) {
            User user = resolveUser(auth);
            Merchant merchant = merchantRepository.findByUser(user).orElse(null);
            if (merchant == null || !merchant.getMerchantId().equals(invoice.getMerchantId())) {
                throw new AccessDeniedException("You can only view your own invoices.");
            }
        }

        return ResponseEntity.ok(invoice);
    }

    // ── Invoices by Merchant (UC-18 list, UC-19 balance) ──────────────────────

    /**
     * GET /api/invoices/merchant/{merchantId}
     * Returns all invoices for a merchant with payment summaries.
     * Powers the "My Balance" screen (merchant) and the Accountant's
     * invoice list filtered by merchant.
     *
     * Merchants can only query their own invoices.
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByMerchant(
            @PathVariable Integer merchantId,
            Authentication auth) {

        if (hasRole(auth, User.Role.MERCHANT)) {
            User user = resolveUser(auth);
            Merchant merchant = merchantRepository.findByUser(user).orElse(null);
            if (merchant == null || !merchant.getMerchantId().equals(merchantId)) {
                throw new AccessDeniedException("You can only view your own invoices.");
            }
        } else {
            requireRole(auth, User.Role.ACCOUNTANT, User.Role.ADMIN, User.Role.MANAGER);
        }

        return ResponseEntity.ok(paymentService.getInvoicesByMerchant(merchantId));
    }

    // ── Unpaid Invoices (Accountant record payment dropdown) ──────────────────

    /**
     * GET /api/invoices/merchant/{merchantId}/unpaid
     * Returns only invoices with an outstanding balance for a merchant.
     * Used by the Accountant's "Record Payment" form to populate the
     * invoice dropdown — only invoices that still need payment are shown.
     */
    @GetMapping("/merchant/{merchantId}/unpaid")
    public ResponseEntity<List<InvoiceDTO>> getUnpaidInvoices(
            @PathVariable Integer merchantId,
            Authentication auth) {

        requireRole(auth, User.Role.ACCOUNTANT, User.Role.ADMIN);

        return ResponseEntity.ok(paymentService.getUnpaidInvoicesByMerchant(merchantId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User resolveUser(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + username));
    }

    private void requireRole(Authentication auth, User.Role... permitted) {
        String roleStr = auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
        User.Role callerRole = User.Role.valueOf(roleStr);

        for (User.Role r : permitted) {
            if (r == callerRole) return;
        }
        throw new AccessDeniedException(
                "Role " + callerRole + " is not permitted to perform this action.");
    }

    private boolean hasRole(Authentication auth, User.Role role) {
        String roleStr = auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
        return role.name().equals(roleStr);
    }
}