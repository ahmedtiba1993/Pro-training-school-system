package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingRequest(
    @Size(max = 1000, message = "SPECIALTY_TRAINING_DESCRIPTION_TOO_LONG")
        String description,
    @NotNull(message = "SPECIALTY_TRAINING_TYPE_REQUIRED")
        TrainingType trainingType,
    @NotNull(message = "SPECIALTY_TRAINING_DURATION_VALUE_REQUIRED")
        @Min(value = 1, message = "SPECIALTY_TRAINING_DURATION_VALUE_POSITIVE")
        Integer durationValue,
    @NotNull(message = "SPECIALTY_TRAINING_DURATION_UNIT_REQUIRED")
        DurationUnit durationUnit,
    @NotNull(message = "SPECIALTY_TRAINING_LEVEL_ID_REQUIRED")
        Long levelId,
    @NotNull(message = "SPECIALTY_TRAINING_SPECIALTY_ID_REQUIRED")
        Long specialtyId) {

  public TrainingRequest {
    if (description != null) {
      description = description.trim();
    }
  }
}
