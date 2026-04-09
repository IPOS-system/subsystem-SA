package com.ipos.ipos_sa.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for GET /api/notifications. Loaded by the Admin and Manager dashboards on startup to
 * display warning banners without navigating to individual screens.
 */
@Data
@Builder
public class NotificationDTO {

  /** True if any active product is below its minimum stock level. */
  private boolean lowStockWarning;

  /** Number of products currently below minimum stock level. */
  private int lowStockCount;

  /** Merchants currently in IN_DEFAULT state — shown as a warning list. */
  private List<DefaultedMerchantDTO> defaultedMerchants;

  @Data
  @Builder
  public static class DefaultedMerchantDTO {
    private Integer merchantId;
    private String companyName;
    private String accountStatus;
  }
}
