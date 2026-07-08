package com.tiba.pts.modules.examscheduling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamTimetableRequest(
    @NotBlank(message = "NAME_REQUIRED") String name,
    @NotNull(message = "CLASS_GROUP_ID_REQUIRED") Long classGroupId,
    @NotNull(message = "PERIOD_ID_REQUIRED") Long periodId,
    @NotNull(message = "EXAM_SESSION_ID_REQUIRED") Long examSessionId) {

  public ExamTimetableRequest {
    if (name != null) {
      name = name.trim();
    }
  }
}
