package com.ipos.ipos_sa.dto.account;

import com.ipos.ipos_sa.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for POST /api/accounts/staff.
 * Creates a non-merchant user (ADMIN, MANAGER, ACCOUNTANT, DIRECTOR).
 */
@Data
public class CreateStaffRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private User.Role role;
}
