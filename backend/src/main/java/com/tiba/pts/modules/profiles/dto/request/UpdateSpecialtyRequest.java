package com.tiba.pts.modules.profiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSpecialtyRequest {

  @NotBlank(message = "SPECIALTY_LABEL_REQUIRED")
  private String label;

  private String description;
}
