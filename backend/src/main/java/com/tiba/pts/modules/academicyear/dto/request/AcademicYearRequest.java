package com.tiba.pts.modules.academicyear.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AcademicYearRequest(
    @NotBlank(message = "LABEL_REQUIRED") String label,
    @NotNull(message = "START_DATE_REQUIRED") LocalDate startDate,
    @NotNull(message = "END_DATE_REQUIRED") LocalDate endDate) {
  @JsonIgnore
  @AssertTrue(message = "END_DATE_MUST_BE_AFTER_START_DATE")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || endDate.isAfter(startDate);
  }
}
