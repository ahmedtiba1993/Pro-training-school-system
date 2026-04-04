package com.tiba.pts.modules.academicyear.dto.response;

import java.time.LocalDate;

public record HolidayResponse(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    Long numberOfDays,
    Long academicYearId
    ) {}
