package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LevelRequest(
    @NotBlank(message = "CODE_REQUIRED") @Size(max = 10, message = "CODE_TOO_LONG") String code,
    @NotBlank(message = "LABEL_REQUIRED") @Size(max = 100, message = "LABEL_TOO_LONG") String label,
    @NotNull(message = "ACCESS_LEVEL_REQUIRED") AccessLevel accessLevel) {

  public LevelRequest {
    if (code != null) {
      code = code.trim().toUpperCase();
    }

    if (label != null) {
      label = label.trim();
    }
  }
}
