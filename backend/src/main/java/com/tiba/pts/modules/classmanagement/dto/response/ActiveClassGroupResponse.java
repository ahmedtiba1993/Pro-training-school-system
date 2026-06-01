package com.tiba.pts.modules.classmanagement.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.Data;

@Data
public class ActiveClassGroupResponse {
  private Long id;
  private String name;
  private TrainingType trainingType;
}
