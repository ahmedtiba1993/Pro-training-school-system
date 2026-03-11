package com.tiba.pts.modules.specialty.dto;

import com.tiba.pts.modules.specialty.dto.LevelDto;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {
  private Long id;
  private String name;
  private String code;
  private Set<LevelDto> associatedLevels;
}
