package com.tiba.pts.modules.execution.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AttendanceRequest(
    @NotNull(message = "COURSE_SESSION_ID_REQUIRED") Long courseSessionId,
    @NotNull(message = "IS_DRAFT_REQUIRED") Boolean isDraft,
    @NotEmpty(message = "ATTENDANCE_ITEMS_REQUIRED") @Valid List<AttendanceItemRequest> items) {}
