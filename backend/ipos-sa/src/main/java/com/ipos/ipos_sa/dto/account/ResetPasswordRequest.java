package com.ipos.ipos_sa.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for PUT /api/accounts/{id}/reset-password. Admin resets a user's password. The user
 * does not need to know the old one.
 */
@Data
public class ResetPasswordRequest {

  @NotBlank(message = "New password is required")
  private String newPassword;
}
