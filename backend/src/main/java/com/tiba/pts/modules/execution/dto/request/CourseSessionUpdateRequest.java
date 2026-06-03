package com.tiba.pts.modules.execution.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.execution.domain.enums.SessionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CourseSessionUpdateRequest(
    @NotNull(message = "SESSION_DATE_REQUIRED") LocalDate sessionDate,
    @NotNull(message = "START_TIME_REQUIRED") LocalTime startTime,
    @NotNull(message = "END_TIME_REQUIRED") LocalTime endTime,
    @NotNull(message = "SESSION_TYPE_REQUIRED") SessionType sessionType,
    String lessonTitle,
    String courseContent,
    @NotNull(message = "CLASS_GROUP_ID_REQUIRED") Long classGroupId,
    @NotNull(message = "SUBJECT_ID_REQUIRED") Long subjectId,
    @NotNull(message = "TEACHER_ID_REQUIRED") Long teacherId,
    Long roomId
) {
  public CourseSessionUpdateRequest {
    lessonTitle = lessonTitle != null ? lessonTitle.trim() : null;
    courseContent = courseContent != null ? courseContent.trim() : null;
  }

  @JsonIgnore
  @AssertTrue(message = "START_TIME_MUST_BE_BEFORE_END_TIME")
  public boolean isTimeRangeValid() {
    return startTime == null || endTime == null || startTime.isBefore(endTime);
  }
}
