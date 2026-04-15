package com.ipos.ipos_sa.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for the debtor reminder generation endpoint.
 *
 * <p>Per the marking sheet: "Generating reminders to merchant debtors (shown on CA screen) every
 * time a merchant debtor tries to log in." (4 marks)
 *
 * <p>This DTO contains a list of merchants who have overdue invoices, along with the reminder text
 * and outstanding amounts. The frontend (or CA subsystem) can display these on screen and optionally
 * send them via SMTP through PU.
 */
@Data
@Builder
public class DebtorReminderDTO {

  private LocalDateTime generatedAt;
  private List<ReminderLineDTO> reminders;

  @Data
  @Builder
  public static class ReminderLineDTO {

    private Integer merchantId;
    private String companyName;
    private String address;
    private String phone;
    private String email;
    private String accountStatus;

    /** The overdue invoice that triggered this reminder. */
    private String invoiceId;
    private String orderId;
    private BigDecimal amountDue;
    private LocalDate dueDate;
    private long daysOverdue;

    /**
     * "FIRST" or "SECOND" — first reminder is generated when 1–15 days overdue (account still
     * NORMAL or just moved to SUSPENDED), second when 15–30+ days overdue (SUSPENDED or
     * IN_DEFAULT).
     */
    private String reminderType;

    /** Pre-generated reminder text matching the Appendix 6 layout. */
    private String reminderText;
  }
}
