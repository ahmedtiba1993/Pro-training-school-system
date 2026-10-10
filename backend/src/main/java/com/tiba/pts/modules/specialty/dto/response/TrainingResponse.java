package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingResponse {
  private Long id;
  private String code;
  private String description;
  private TrainingType trainingType;
  private Integer durationValue;
  private DurationUnit durationUnit;
  private TrainingStatus status;

  // --- Level details ---
  private Long levelId;
  private String levelCode;
  private String levelLabel;

  // --- Specialty details ---
  private Long specialtyId;
  private String specialtyCode;
  private String specialtyLabel;
}
