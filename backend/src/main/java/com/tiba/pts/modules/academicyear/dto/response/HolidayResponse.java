package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.HolidayType;
import java.time.LocalDate;

public record HolidayResponse(
    Long id,
    String label,
    LocalDate startDate,
    LocalDate endDate,
    HolidayType type,
    Long academicYearId,
    long numberOfDays) {}
