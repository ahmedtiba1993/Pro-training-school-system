package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import lombok.Data;

@Data
public class LevelResponse {
  private Long id;
  private String code;
  private String label;
  private AccessLevel accessLevel;
}
