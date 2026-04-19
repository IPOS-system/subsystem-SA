package com.ipos.ipos_sa.dto.account;

import com.ipos.ipos_sa.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a non-merchant (staff) user account. Returned by GET /api/accounts/staff and GET
 * /api/accounts/staff/{id}.
 */
@Data
@Builder
public class StaffDTO {

  private Integer userId;
  private String username;
  private User.Role role;
  private Boolean isActive;
  private LocalDateTime createdAt;
}
