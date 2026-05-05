package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.SessionType;

import java.time.LocalDate;

public record ExamSessionResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isLocked,
    SessionType sessionType) {}
