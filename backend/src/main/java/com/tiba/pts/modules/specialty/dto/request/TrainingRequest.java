package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingRequest {

  @NotNull(message = "TRAINING_TYPE_REQUIRED")
  private TrainingType trainingType;

  @NotNull(message = "DURATION_IN_MONTHS_REQUIRED")
  @Min(value = 1, message = "DURATION_MIN_1_MONTH")
  private Integer durationInMonths;

  @NotNull(message = "ACTIVE_STATUS_REQUIRED")
  private Boolean isActive;

  @NotNull(message = "LEVEL_ID_REQUIRED")
  private Long levelId;

  @NotNull(message = "SPECIALTY_ID_REQUIRED")
  private Long specialtyId;
}
