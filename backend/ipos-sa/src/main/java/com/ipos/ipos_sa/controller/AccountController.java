package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.account.*;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.AccountService;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for account management.
 *
 * Role rules enforced here:
 *   GET  merchants / staff          → ADMIN, MANAGER
 *   POST merchant account           → ADMIN only
 *   POST staff account              → ADMIN only
 *   PUT  merchant details           → ADMIN, MANAGER
 *   PUT  credit limit / plan        → ADMIN, MANAGER  (handled inside updateMerchantDetails)
 *   PUT  deactivate / reactivate    → ADMIN only
 *   PUT  restore from IN_DEFAULT    → ADMIN, DIRECTOR
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService  accountService;
    private final UserRepository  userRepository;

    // ── Merchant Endpoints ────────────────────────────────────────────────────

    /**
     * GET /api/accounts/merchants
     * Returns all merchant accounts. Optionally filter with ?search=term
     */
    @GetMapping("/merchants")
    public ResponseEntity<List<MerchantDTO>> getMerchants(
            @RequestParam(required = false) String search,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);

        List<MerchantDTO> result = (search != null && !search.isBlank())
                ? accountService.searchMerchants(search)
                : accountService.getAllMerchants();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/accounts/merchants/{id}
     * Returns a single merchant by ID.
     */
    @GetMapping("/merchants/{id}")
    public ResponseEntity<MerchantDTO> getMerchant(
            @PathVariable Integer id,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        return ResponseEntity.ok(accountService.getMerchantById(id));
    }

    /**
     * POST /api/accounts/merchant
     * Creates a new merchant account. ADMIN only.
     */
    @PostMapping("/merchant")
    public ResponseEntity<MerchantDTO> createMerchant(
            @Valid @RequestBody CreateMerchantRequest request,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);
        MerchantDTO created = accountService.createMerchantAccount(request, actingUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/accounts/merchants/{id}
     * Updates merchant contact details, credit limit, or discount plan.
     * ADMIN and MANAGER can update contact fields.
     * Credit limit and discount plan changes also require ADMIN or MANAGER.
     */
    @PutMapping("/merchants/{id}")
    public ResponseEntity<MerchantDTO> updateMerchant(
            @PathVariable Integer id,
            @RequestBody UpdateMerchantRequest request,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        User actingUser = resolveUser(auth);
        return ResponseEntity.ok(accountService.updateMerchantDetails(id, request, actingUser));
    }

    /**
     * PUT /api/accounts/merchants/{id}/restore
     * Restores a merchant from IN_DEFAULT to NORMAL.
     * ADMIN and DIRECTOR only, per the brief (Director of Operations authorisation).
     */
    @PutMapping("/merchants/{id}/restore")
    public ResponseEntity<MerchantDTO> restoreMerchant(
            @PathVariable Integer id,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.DIRECTOR);
        User actingUser = resolveUser(auth);
        return ResponseEntity.ok(accountService.restoreFromDefault(id, actingUser));
    }

    // ── Staff Endpoints ───────────────────────────────────────────────────────

    /**
     * GET /api/accounts/staff
     * Returns all non-merchant (staff) accounts. ADMIN only.
     */
    @GetMapping("/staff")
    public ResponseEntity<List<StaffDTO>> getStaff(Authentication auth) {
        requireRole(auth, User.Role.ADMIN);
        return ResponseEntity.ok(accountService.getAllStaff());
    }

    /**
     * POST /api/accounts/staff
     * Creates a new staff account. ADMIN only.
     */
    @PostMapping("/staff")
    public ResponseEntity<StaffDTO> createStaff(
            @Valid @RequestBody CreateStaffRequest request,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);
        StaffDTO created = accountService.createStaffAccount(request, actingUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Shared Deactivate / Reactivate ────────────────────────────────────────

    /**
     * PUT /api/accounts/{id}/deactivate
     * Soft-deletes any account (merchant or staff). ADMIN only.
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Integer id,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);
        accountService.deactivateAccount(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/accounts/{id}/reactivate
     * Re-activates a deactivated account. ADMIN only.
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable Integer id,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);
        accountService.reactivateAccount(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves the currently authenticated User entity from the security context.
     * The JWT filter stores the username as the principal — we look up the full
     * User record from the database so services have access to the user ID for
     * audit logging.
     */
    private User resolveUser(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + username));
    }

    /**
     * Throws AccessDeniedException if the authenticated user's role is not
     * in the list of permitted roles.
     */
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
}
