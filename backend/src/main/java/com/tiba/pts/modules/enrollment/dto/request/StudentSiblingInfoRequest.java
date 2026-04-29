package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentSiblingInfoRequest {
  @NotBlank(message = "SIBLING_FULL_NAME_REQUIRED")
  private String fullName;

  @NotNull(message = "SIBLING_AGE_REQUIRED")
  private Integer age;

  private String schoolOrWorkplace;
}
