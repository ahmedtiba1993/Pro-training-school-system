package com.tiba.pts.modules.specialty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyRequest {

  @NotBlank(message = "SPECIALTY_NAME_REQUIRED")
  private String name;

  @NotBlank(message = "SPECIALTY_CODE_REQUIRED")
  private String code;

  @NotEmpty(message = "LEVEL_IDS_REQUIRED")
  private Set<Long> levelIds;
}
