package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public abstract class PersonInfoRequest {
  @NotBlank(message = "FIRST_NAME_REQUIRED")
  private String firstName;

  @NotBlank(message = "LAST_NAME_REQUIRED")
  private String lastName;

  private String firstNameAr;
  private String lastNameAr;
  private String email;
  private String phone;
  private String cin;
}
