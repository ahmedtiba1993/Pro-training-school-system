package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelResponse {
  private Long id;
  private String code;
  private String label;
  private Boolean isActive;
  private AccessLevel accessLevel;
}
