package com.tiba.pts.modules.trainingsession.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceleratedPromotionRequest extends BasePromotionRequest {

  @NotNull(message = "START_DATE_REQUIRED")
  private LocalDate startDate;

  @NotNull(message = "END_DATE_REQUIRED")
  private LocalDate endDate;

  @JsonIgnore
  @AssertTrue(message = "END_DATE_MUST_BE_AFTER_START_DATE")
  public boolean isEndDateAfterStartDate() {
    if (startDate == null || endDate == null) {
      return true;
    }
    return endDate.isAfter(startDate);
  }
}
