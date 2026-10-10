package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LevelRequest(
    @NotBlank(message = "SPECIALTY_LEVEL_CODE_REQUIRED")
        @Size(max = 10, message = "SPECIALTY_LEVEL_CODE_TOO_LONG")
        String code,
    @NotBlank(message = "SPECIALTY_LEVEL_LABEL_REQUIRED")
        @Size(max = 100, message = "SPECIALTY_LEVEL_LABEL_TOO_LONG")
        String label,
    @NotNull(message = "SPECIALTY_LEVEL_ACCESS_LEVEL_REQUIRED")
        AccessLevel accessLevel) {

  public LevelRequest {
    if (code != null) {
      code = code.trim().toUpperCase();
    }
    if (label != null) {
      label = label.trim();
    }
  }
}
