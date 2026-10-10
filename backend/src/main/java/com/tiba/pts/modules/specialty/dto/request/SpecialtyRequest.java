package com.tiba.pts.modules.specialty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyRequest(
    @NotBlank(message = "SPECIALTY_CODE_REQUIRED")
        @Size(max = 10, message = "SPECIALTY_CODE_TOO_LONG")
        String code,
    @NotBlank(message = "SPECIALTY_LABEL_REQUIRED")
        @Size(max = 100, message = "SPECIALTY_LABEL_TOO_LONG")
        String label) {

  public SpecialtyRequest {
    if (code != null) {
      code = code.trim().toUpperCase();
    }
    if (label != null) {
      label = label.trim();
    }
  }
}
