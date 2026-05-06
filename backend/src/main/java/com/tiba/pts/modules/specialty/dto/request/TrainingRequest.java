package com.tiba.pts.modules.specialty.dto.request;

import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingRequest(
    @Size(max = 1000, message = "DESCRIPTION_TOO_LONG") String description,
    @NotNull(message = "TRAINING_TYPE_REQUIRED") TrainingType trainingType,
    @NotNull(message = "DURATION_VALUE_REQUIRED")
        @Min(value = 1, message = "DURATION_VALUE_MUST_BE_POSITIVE")
        Integer durationValue,
    @NotNull(message = "DURATION_UNIT_REQUIRED") DurationUnit durationUnit,
    @NotNull(message = "LEVEL_ID_REQUIRED") Long levelId,
    @NotNull(message = "SPECIALTY_ID_REQUIRED") Long specialtyId) {

  public TrainingRequest {

    if (description != null) {
      description = description.trim();
    }
  }
}
