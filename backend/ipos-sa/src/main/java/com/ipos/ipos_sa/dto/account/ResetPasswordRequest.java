package com.ipos.ipos_sa.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for PUT /api/accounts/{id}/reset-password(Admin only).
 */
@Data
public class ResetPasswordRequest {

  @NotBlank(message = "New password is required")
  private String newPassword;
}
