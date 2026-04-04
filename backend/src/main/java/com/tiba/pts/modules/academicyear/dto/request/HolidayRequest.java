package com.tiba.pts.modules.academicyear.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HolidayRequest(
    @NotBlank(message = "TITLE_REQUIRED") String title,
    @NotNull(message = "START_DATE_REQUIRED") LocalDate startDate,
    LocalDate endDate,
    @NotNull(message = "ACADEMIC_YEAR_ID_REQUIRED") Long academicYearId) {

  @JsonIgnore
  @AssertTrue(message = "END_DATE_MUST_BE_AFTER_START_DATE")
  public boolean isDateRangeValid() {
    // If startDate is null (handled by @NotNull) or if endDate is null (single day),
    if (startDate == null || endDate == null) {
      return true;
    }

    // Only perform the check if both dates are present
    return endDate.isAfter(startDate);
  }
}
