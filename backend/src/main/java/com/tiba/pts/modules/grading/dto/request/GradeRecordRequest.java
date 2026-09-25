package com.tiba.pts.modules.grading.dto.request;

import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GradeRecordRequest(
    @NotNull(message = "ASSESSMENT_ID_REQUIRED")
    Long assessmentId,

    @NotNull(message = "ENROLLMENT_ID_REQUIRED")
    Long enrollmentId,

    BigDecimal score,

    @NotNull(message = "ATTENDANCE_STATUS_REQUIRED")
    AttendanceStatus attendanceStatus,

    String teacherComment,

    Boolean isOverridden,

    String overrideReason
) {
  public GradeRecordRequest {
    isOverridden = isOverridden != null ? isOverridden : Boolean.FALSE;
    overrideReason = overrideReason != null ? overrideReason.trim() : null;
    teacherComment = teacherComment != null ? teacherComment.trim() : null;
  }
}
