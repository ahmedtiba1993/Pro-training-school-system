package com.tiba.pts.modules.examscheduling.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamScheduleRequest(
    @NotNull(message = "ASSESSMENT_ID_REQUIRED") Long assessmentId,
    @NotNull(message = "EXAM_DATE_REQUIRED") LocalDate examDate,
    @NotNull(message = "EXAM_TIME_SLOT_ID_REQUIRED") Long examTimeSlotId,
    Long roomId,
    Integer capacityUsed) {}
