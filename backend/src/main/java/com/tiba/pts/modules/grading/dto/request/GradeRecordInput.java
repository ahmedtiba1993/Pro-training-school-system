package com.tiba.pts.modules.grading.dto.request;

import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GradeRecordInput(
    @NotNull(message = "ENROLLMENT_ID_REQUIRED")
    Long enrollmentId,

    BigDecimal score,

    @NotNull(message = "ATTENDANCE_STATUS_REQUIRED")
    AttendanceStatus attendanceStatus,

    String teacherComment
) {}
