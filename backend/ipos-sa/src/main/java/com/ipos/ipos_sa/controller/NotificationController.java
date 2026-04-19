package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.NotificationDTO;
import com.ipos.ipos_sa.entity.CatalogueItem;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.service.CatalogueService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard notifications.
 *
 * Called by the C# frontend on dashboard startup (Admin and Manager dashboards) to display
 * warning banners without the user having to navigate to individual screens.
 *
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

  private final CatalogueService catalogueService;
  private final MerchantRepository merchantRepository;

  private boolean hasRole(Authentication auth, User.Role role) {
  String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
  return role.name().equals(roleStr);
  }


  @GetMapping
  public ResponseEntity<NotificationDTO> getNotifications(Authentication auth) {

  // Non-admin/manager callers (e.g. MERCHANT) get a safe empty response
  // rather than a 403, so the frontend can call this unconditionally.
  if (!hasRole(auth, User.Role.ADMIN) && !hasRole(auth, User.Role.MANAGER)) {
    return ResponseEntity.ok(
        NotificationDTO.builder()
            .lowStockWarning(false)
            .lowStockCount(0)
            .defaultedMerchants(java.util.Collections.emptyList())
            .build());
  }

  // Low stock check
  List<CatalogueItem> lowStockItems = catalogueService.getLowStockProducts();

  // In-default merchants
  List<Merchant> defaultedMerchants =
      merchantRepository.findByAccountStatus(Merchant.AccountStatus.IN_DEFAULT);

  List<NotificationDTO.DefaultedMerchantDTO> defaultedDTOs =
      defaultedMerchants.stream()
          .map(
              m ->
                  NotificationDTO.DefaultedMerchantDTO.builder()
                      .merchantId(m.getMerchantId())
                      .companyName(m.getCompanyName())
                      .accountStatus(m.getAccountStatus().name())
                      .build())
          .collect(Collectors.toList());

  NotificationDTO response =
      NotificationDTO.builder()
          .lowStockWarning(!lowStockItems.isEmpty())
          .lowStockCount(lowStockItems.size())
          .defaultedMerchants(defaultedDTOs)
          .build();

    return ResponseEntity.ok(response);
  }

  
  // Helpers

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
