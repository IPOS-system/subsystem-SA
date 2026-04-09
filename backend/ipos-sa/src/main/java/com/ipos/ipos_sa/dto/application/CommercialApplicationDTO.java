package com.ipos.ipos_sa.dto.application;

import com.ipos.ipos_sa.entity.CommercialApplication;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a commercial membership application. Returned by POST
 * /api/applications/commercial (to IPOS-PU) and GET /api/applications (to Admin).
 */
@Data
@Builder
public class CommercialApplicationDTO {

  private Integer applicationId;
  private String companyName;
  private String companyRegNo;
  private String directors;
  private String businessType;
  private String address;
  private String email;
  private String phone;
  private CommercialApplication.ApplicationStatus status;
  private LocalDateTime submittedAt;
  private Integer reviewedBy;
  private LocalDateTime reviewedAt;
  private String notes;
}
