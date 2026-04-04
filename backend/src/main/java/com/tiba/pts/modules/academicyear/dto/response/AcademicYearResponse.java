package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;

import java.time.LocalDate;

public record AcademicYearResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    boolean isActive,
    YearStatus status) {}
