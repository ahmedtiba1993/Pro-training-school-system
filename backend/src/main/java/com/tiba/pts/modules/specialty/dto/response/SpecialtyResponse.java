package com.tiba.pts.modules.specialty.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {
  private Long id;
  private String label;
  private String code;
}
