package com.tiba.pts.modules.academicyear.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PeriodResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    Long academicYearId,
    List<ExamSessionResponse> sessions) {}
