package com.tiba.pts.modules.examscheduling.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExamRoomAllocationRequest(
    @NotNull(message = "ROOM_ID_REQUIRED") Long roomId,
    @NotNull(message = "CAPACITY_USED_REQUIRED")
        @Min(value = 1, message = "CAPACITY_USED_MUST_BE_GREATER_THAN_ZERO")
        Integer capacityUsed) {}
