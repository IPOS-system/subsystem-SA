package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.NotificationDTO;
import com.ipos.ipos_sa.entity.CatalogueItem;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for dashboard notifications.
 *
 * Called by the C# frontend on dashboard startup (Admin and Manager dashboards)
 * to display warning banners without the user having to navigate to individual
 * screens.
 *
 * Per the SA-APPLICATION plan:
 *   - Admin dashboard: low-stock warnings
 *   - Manager dashboard: low-stock warnings + in-default merchant account warnings
 *
 * Role rules:
 *   GET /api/notifications → ADMIN, MANAGER
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final CatalogueService   catalogueService;
    private final MerchantRepository merchantRepository;

    /**
     * GET /api/notifications
     * Returns a summary of active warnings for the dashboard.
     *
     * Response includes:
     *   - lowStockWarning: true if any active product is below its min stock level
     *   - lowStockCount: how many products are below threshold
     *   - defaultedMerchants: list of merchants currently IN_DEFAULT
     */
    @GetMapping
    public ResponseEntity<NotificationDTO> getNotifications(Authentication auth) {

        requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);

        // Low stock check (UC-35)
        List<CatalogueItem> lowStockItems = catalogueService.getLowStockProducts();

        // In-default merchants (for Manager warning banner)
        List<Merchant> defaultedMerchants = merchantRepository
                .findByAccountStatus(Merchant.AccountStatus.IN_DEFAULT);

        List<NotificationDTO.DefaultedMerchantDTO> defaultedDTOs = defaultedMerchants.stream()
                .map(m -> NotificationDTO.DefaultedMerchantDTO.builder()
                        .merchantId(m.getMerchantId())
                        .companyName(m.getCompanyName())
                        .accountStatus(m.getAccountStatus().name())
                        .build())
                .collect(Collectors.toList());

        NotificationDTO response = NotificationDTO.builder()
                .lowStockWarning(!lowStockItems.isEmpty())
                .lowStockCount(lowStockItems.size())
                .defaultedMerchants(defaultedDTOs)
                .build();

        return ResponseEntity.ok(response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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