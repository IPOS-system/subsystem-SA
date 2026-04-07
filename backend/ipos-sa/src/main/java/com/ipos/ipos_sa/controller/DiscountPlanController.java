package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.discount.*;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.DiscountPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for discount plan management.
 *
 * Role rules:
 *   GET  /api/discount-plans        → ADMIN, MANAGER (populate dropdowns)
 *   GET  /api/discount-plans/{id}   → ADMIN, MANAGER (view plan detail)
 *   POST /api/discount-plans/fixed  → ADMIN, MANAGER (create fixed plan)
 *   POST /api/discount-plans/flexible → ADMIN, MANAGER (create flexible plan)
 */
@RestController
@RequestMapping("/api/discount-plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DiscountPlanController {

    private final DiscountPlanService discountPlanService;
    private final UserRepository      userRepository;

    /**
     * GET /api/discount-plans
     * Returns all discount plans. Used to populate the dropdown on
     * merchant account creation and edit screens.
     */
    @GetMapping
    public ResponseEntity<List<DiscountPlanDTO>> getAllPlans(Authentication auth) {
        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        return ResponseEntity.ok(discountPlanService.getAllPlans());
    }

    /**
     * GET /api/discount-plans/{id}
     * Returns a single plan with full tier detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiscountPlanDTO> getPlan(
            @PathVariable Integer id,
            Authentication auth) {
        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        return ResponseEntity.ok(discountPlanService.getPlanById(id));
    }

    /**
     * POST /api/discount-plans/fixed
     * Creates a fixed-rate discount plan (UC-8 support).
     */
    @PostMapping("/fixed")
    public ResponseEntity<DiscountPlanDTO> createFixedPlan(
            @Valid @RequestBody CreateFixedPlanRequest request,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        User actingUser = resolveUser(auth);
        DiscountPlanDTO created = discountPlanService.createFixedPlan(request, actingUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/discount-plans/flexible
     * Creates a tiered (flexible) discount plan (UC-8 support).
     */
    @PostMapping("/flexible")
    public ResponseEntity<DiscountPlanDTO> createFlexiblePlan(
            @Valid @RequestBody CreateFlexiblePlanRequest request,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
        User actingUser = resolveUser(auth);
        DiscountPlanDTO created = discountPlanService.createFlexiblePlan(request, actingUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
}