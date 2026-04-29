package com.tiba.pts.modules.profiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentSiblingRequest {
  @NotBlank(message = "SIBLING_FULL_NAME_REQUIRED")
  private String fullName;

  @NotNull(message = "SIBLING_AGE_REQUIRED")
  private Integer age;

  private String schoolOrWorkplace;
}
