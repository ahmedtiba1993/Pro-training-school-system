package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;

import java.time.LocalDate;

public record ActiveAcademicYearResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    YearStatus status,

    // --- Global Statistics ---
    long remainingDaysInYear, // Days before the end of the school year

    // --- Current Period Statistics ---
    String currentPeriodLabel,
    int periodProgress,
    long remainingDaysInCurrentPeriod) {}
