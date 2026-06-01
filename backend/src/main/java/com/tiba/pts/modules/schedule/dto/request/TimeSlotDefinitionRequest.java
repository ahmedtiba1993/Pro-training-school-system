package com.tiba.pts.modules.schedule.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record TimeSlotDefinitionRequest(
    @NotBlank(message = "CODE_REQUIRED") String code,
    @NotBlank(message = "LABEL_REQUIRED") String label,
    @NotNull(message = "START_TIME_REQUIRED") LocalTime startTime,
    @NotNull(message = "END_TIME_REQUIRED") LocalTime endTime,
    @NotNull(message = "ORDER_INDEX_REQUIRED") Integer orderIndex) {
  // Compact constructor to clean spaces and force uppercase
  public TimeSlotDefinitionRequest {
    code = code != null ? code.replaceAll("\\s+", "").toUpperCase() : null;
    label = label != null ? label.replaceAll("\\s+", "") : null;
  }

  // Test time consistency directly in the DTO
  @AssertTrue(message = "END_TIME_MUST_BE_AFTER_START_TIME")
  public boolean isTimeWindowValid() {
    if (startTime == null || endTime == null) {
      return true; // Let the @NotNull validation handle it
    }
    return endTime.isAfter(startTime);
  }
}
