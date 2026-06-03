package com.tiba.pts.modules.execution.dto.request;

import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceItemRequest(
    @NotNull(message = "ENROLLMENT_ID_REQUIRED")
    Long enrollmentId,

    @NotNull(message = "ATTENDANCE_STATUS_REQUIRED")
    AttendanceStatus status
) {}
