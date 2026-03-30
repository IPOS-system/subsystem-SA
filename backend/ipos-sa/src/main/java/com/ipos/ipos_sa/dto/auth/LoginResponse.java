package com.ipos.ipos_sa.dto.auth;

import com.ipos.ipos_sa.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/auth/login.
 *
 * The C# frontend uses {@code role} to decide which dashboard window to open.
 * {@code paymentReminderDue} is only meaningful for MERCHANT role — it is {@code true}
 * when the merchant has at least one invoice that is 1–15 days overdue (account
 * is still NORMAL but a reminder banner should be shown on the dashboard).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** The authenticated user's primary key. */
    private Integer userId;

    private String username;

    /** Determines which dashboard the C# app should display. */
    private User.Role role;

    /**
     * For MERCHANT role only: {@code true} if any invoice is 1–15 days past its
     * due date (account still NORMAL). The C# dashboard shows a reminder banner.
     * Always {@code false} for non-merchant roles.
     */
    private boolean paymentReminderDue;

    /**
     * For MERCHANT role only: the merchant's internal ID.
     * Used by the C# frontend to scope subsequent API calls.
     * {@code null} for non-merchant users.
     */
    private Integer merchantId;

    /** JWT authentication token for subsequent API requests. */
    private String token;
}
