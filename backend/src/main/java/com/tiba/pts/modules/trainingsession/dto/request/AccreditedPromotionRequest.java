package com.tiba.pts.modules.trainingsession.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AccreditedPromotionRequest extends BasePromotionRequest {

  @NotNull(message = "ACADEMIC_YEAR_ID_IS_REQUIRED")
  private Long academicYearId;
}
