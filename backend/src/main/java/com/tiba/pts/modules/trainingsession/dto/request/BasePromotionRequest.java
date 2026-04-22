package com.tiba.pts.modules.trainingsession.dto.request;

import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public abstract class BasePromotionRequest {

  @NotBlank(message = "NAME_IS_REQUIRED")
  private String name;

  @NotBlank(message = "CODE_IS_REQUIRED")
  private String code;

  @NotNull(message = "START_DATE_IS_REQUIRED")
  private LocalDate startDate;

  @NotNull(message = "END_DATE_IS_REQUIRED")
  private LocalDate endDate;

  @NotNull(message = "FEE_IS_REQUIRED")
  @Positive(message = "FEE_MUST_BE_POSITIVE")
  private Double fee;

  @NotNull(message = "TRAINING_ID_IS_REQUIRED")
  private Long trainingId;

  @AssertTrue(message = "START_DATE_MUST_BE_BEFORE_END_DATE")
  private boolean isDateValid() {
    if (startDate == null || endDate == null) {
      return true;
    }
    return !startDate.isAfter(endDate);
  }
}
