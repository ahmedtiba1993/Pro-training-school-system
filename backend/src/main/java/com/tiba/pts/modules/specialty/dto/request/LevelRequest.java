package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LevelRequest {

  @NotBlank(message = "CODE_REQUIRED")
  @Size(max = 20, message = "CODE_TOO_LONG")
  private String code;

  @NotBlank(message = "LABEL_REQUIRED")
  @Size(max = 100, message = "LABEL_TOO_LONG")
  private String label;

  @NotNull(message = "ACCESS_LEVEL_REQUIRED")
  private AccessLevel accessLevel;
}
