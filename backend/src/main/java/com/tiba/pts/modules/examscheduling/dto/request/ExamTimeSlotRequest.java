package com.tiba.pts.modules.examscheduling.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ExamTimeSlotRequest(
    @NotBlank(message = "CODE_REQUIRED") String code,
    @NotBlank(message = "LABEL_REQUIRED") String label,
    @NotNull(message = "START_TIME_REQUIRED") LocalTime startTime,
    @NotNull(message = "END_TIME_REQUIRED") LocalTime endTime) {

  public ExamTimeSlotRequest {
    if (code != null) {
      code = code.trim().toUpperCase();
    }
    if (label != null) {
      label = label.trim();
    }
  }

  @JsonIgnore
  @AssertTrue(message = "START_TIME_MUST_BE_BEFORE_END_TIME")
  public boolean isTimeWindowValid() {
    return startTime == null || endTime == null || startTime.isBefore(endTime);
  }
}
