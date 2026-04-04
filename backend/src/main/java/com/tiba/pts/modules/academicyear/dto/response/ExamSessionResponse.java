package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.SessionStatus;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;

import java.time.LocalDate;

public record ExamSessionResponse(
    Long id,
    String label,
    SessionType sessionType,
    LocalDate startDate,
    LocalDate endDate,
    SessionStatus status,
    Long periodId
    ) {}
