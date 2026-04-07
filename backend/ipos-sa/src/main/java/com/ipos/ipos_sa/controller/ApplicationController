package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.application.CommercialApplicationDTO;
import com.ipos.ipos_sa.dto.application.SubmitApplicationRequest;
import com.ipos.ipos_sa.entity.CommercialApplication;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.CommercialApplicationRepository;
import com.ipos.ipos_sa.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for commercial membership applications (ICMAAPI).
 *
 * Inter-subsystem: IPOS-PU calls POST /api/applications/commercial
 * to submit a commercial membership application (submitCA).
 *
 * Admin: GET /api/applications to review pending applications,
 * PUT /api/applications/{id}/approve or /reject to process them.
 *
 * Role rules:
 *   POST /api/applications/commercial → any authenticated user (IPOS-PU service account)
 *   GET  /api/applications            → ADMIN, MANAGER
 *   PUT  /api/applications/{id}/...   → ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final CommercialApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    // ── submitCA — Inter-Subsystem Endpoint (ICMAAPI) ─────────────────────────

    /**
     * POST /api/applications/commercial
     * Receives a commercial membership application from IPOS-PU.
     *
     * Maps to: ICMAAPI.submitCA(CommercialApplicationForm)
     *
     * IPOS-PU authenticates first via POST /api/auth/login using a
     * service account, then calls this endpoint with the Bearer token.
     *
     * Returns the application ID as confirmation (matching the interface
     * return type of String).
     */
    @PostMapping("/commercial")
    public ResponseEntity<CommercialApplicationDTO> submitCommercialApplication(
            @Valid @RequestBody SubmitApplicationRequest request) {

        CommercialApplication application = CommercialApplication.builder()
                .companyName(request.getCompanyName())
                .companyRegNo(request.getCompanyRegNo())
                .directors(request.getDirectors())
                .businessType(request.getBusinessType())
                .address(request.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(CommercialApplication.ApplicationStatus.PENDING)
                .build();

        applicationRepository.save(application);

        log.info("Commercial application submitted: {} (ID={})",
                request.getCompanyName(), application.getApplicationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(application));
    }

    // ── Admin: List Applications ──────────────────────────────────────────────

    /**
     * GET /api/applications
     * Returns all applications, optionally filtered by ?status=PENDING.
     */
    @GetMapping
    public ResponseEntity<List<CommercialApplicationDTO>> listApplications(
            @RequestParam(required = false) CommercialApplication.ApplicationStatus status,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);

        List<CommercialApplication> apps = (status != null)
                ? applicationRepository.findByStatusOrderBySubmittedAtDesc(status)
                : applicationRepository.findAllByOrderBySubmittedAtDesc();

        List<CommercialApplicationDTO> result = apps.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Admin: Approve Application ────────────────────────────────────────────

    /**
     * PUT /api/applications/{id}/approve
     * Approves a pending commercial application. The admin would then
     * create a merchant account separately via POST /api/accounts/merchant.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<CommercialApplicationDTO> approveApplication(
            @PathVariable Integer id,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);

        CommercialApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CommercialApplication", id));

        app.setStatus(CommercialApplication.ApplicationStatus.APPROVED);
        app.setReviewedBy(actingUser.getUserId());
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        log.info("Application {} approved by userId={}", id, actingUser.getUserId());
        return ResponseEntity.ok(toDTO(app));
    }

    // ── Admin: Reject Application ─────────────────────────────────────────────

    /**
     * PUT /api/applications/{id}/reject
     * Rejects a pending commercial application.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<CommercialApplicationDTO> rejectApplication(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason,
            Authentication auth) {

        requireRole(auth, User.Role.ADMIN);
        User actingUser = resolveUser(auth);

        CommercialApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CommercialApplication", id));

        app.setStatus(CommercialApplication.ApplicationStatus.REJECTED);
        app.setReviewedBy(actingUser.getUserId());
        app.setReviewedAt(LocalDateTime.now());
        if (reason != null) app.setNotes(reason);
        applicationRepository.save(app);

        log.info("Application {} rejected by userId={}", id, actingUser.getUserId());
        return ResponseEntity.ok(toDTO(app));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private CommercialApplicationDTO toDTO(CommercialApplication app) {
        return CommercialApplicationDTO.builder()
                .applicationId(app.getApplicationId())
                .companyName(app.getCompanyName())
                .companyRegNo(app.getCompanyRegNo())
                .directors(app.getDirectors())
                .businessType(app.getBusinessType())
                .address(app.getAddress())
                .email(app.getEmail())
                .phone(app.getPhone())
                .status(app.getStatus())
                .submittedAt(app.getSubmittedAt())
                .reviewedBy(app.getReviewedBy())
                .reviewedAt(app.getReviewedAt())
                .notes(app.getNotes())
                .build();
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