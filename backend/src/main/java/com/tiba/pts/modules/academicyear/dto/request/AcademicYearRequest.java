package com.tiba.pts.modules.academicyear.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record AcademicYearRequest(
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "INVALID_LABEL_FORMAT")
        @NotBlank(message = "LABEL_REQUIRED")
        String label,
    @NotNull(message = "START_DATE_REQUIRED") LocalDate startDate,
    @NotNull(message = "END_DATE_REQUIRED") LocalDate endDate) {
  @JsonIgnore
  @AssertTrue(message = "END_DATE_MUST_BE_AFTER_START_DATE")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || endDate.isAfter(startDate);
  }

  @JsonIgnore
  @AssertTrue(message = "INVALID_YEAR_GAP")
  public boolean isGapValid() {
    // Ignored if the basic format is invalid (already handled by @Pattern)
    if (label == null || !label.matches("^\\d{4}-\\d{4}$")) {
      return true;
    }
    String[] parts = label.split("-");
    int startYear = Integer.parseInt(parts[0]);
    int endYear = Integer.parseInt(parts[1]);

    // Ensures mathematically that the end year = start year + 1
    return endYear == (startYear + 1);
  }

  @JsonIgnore
  @AssertTrue(message = "INVALID_DURATION_MUST_BE_8_TO_11_MONTHS")
  public boolean isDurationValid() {
    if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
      return true;
    }
    long monthsBetween = ChronoUnit.MONTHS.between(startDate, endDate);
    return monthsBetween >= 8 && monthsBetween <= 11;
  }
}
