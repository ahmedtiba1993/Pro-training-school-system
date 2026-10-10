package com.tiba.pts.modules.specialty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyResponse {
  private Long id;
  private String label;
  private String code;
  private Boolean isActive;
}
