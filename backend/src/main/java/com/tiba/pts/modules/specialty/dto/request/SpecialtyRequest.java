package com.tiba.pts.modules.specialty.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyRequest {

  @NotBlank(message = "SPECIALTY_LABEL_REQUIRED")
  private String label;

  @NotBlank(message = "SPECIALTY_CODE_REQUIRED")
  private String code;
}
