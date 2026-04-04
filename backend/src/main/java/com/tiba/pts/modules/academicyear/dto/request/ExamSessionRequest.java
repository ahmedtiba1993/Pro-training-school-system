package com.tiba.pts.modules.academicyear.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.academicyear.domain.enums.SessionStatus;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamSessionRequest(
    @NotBlank(message = "LABEL_REQUIRED") String label,
    @NotNull(message = "SESSION_TYPE_REQUIRED") SessionType sessionType,
    @NotNull(message = "START_DATE_REQUIRED") LocalDate startDate,
    @NotNull(message = "END_DATE_REQUIRED") LocalDate endDate,
    @NotNull(message = "STATUS_REQUIRED") SessionStatus status,
    @NotNull(message = "PERIOD_ID_REQUIRED") Long periodId) {
  @JsonIgnore
  @AssertTrue(message = "END_DATE_MUST_BE_AFTER_START_DATE")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || endDate.isAfter(startDate);
  }
}
