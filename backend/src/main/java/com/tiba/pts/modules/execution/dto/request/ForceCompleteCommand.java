package com.tiba.pts.modules.execution.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ForceCompleteCommand(
    @NotNull(message = "ATTENDANCE_SUBMISSION_FLAG_REQUIRED")
    @AssertTrue(message = "ATTENDANCE_MUST_BE_SUBMITTED")
    Boolean isAttendanceSubmitted,
    
    String lessonTitle,
    
    String courseContent
) {
}
