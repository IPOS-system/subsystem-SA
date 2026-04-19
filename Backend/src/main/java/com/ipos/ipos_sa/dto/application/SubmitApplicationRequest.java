package com.ipos.ipos_sa.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/applications/commercial.
 * Maps to the CommercialApplicationForm submitted by IPOS-PU.
 */
@Data
public class SubmitApplicationRequest {

  @NotBlank(message = "Company name is required")
  private String companyName;

  @NotBlank(message = "Company registration number is required")
  private String companyRegNo;

  @NotBlank(message = "Directors field is required")
  private String directors;

  @NotBlank(message = "Business type is required")
  private String businessType;

  @NotBlank(message = "Address is required")
  private String address;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  private String phone;
}
