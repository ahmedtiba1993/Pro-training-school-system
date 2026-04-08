package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingTypeCountResponse {
  private TrainingType trainingType;
  private Long count;
}
