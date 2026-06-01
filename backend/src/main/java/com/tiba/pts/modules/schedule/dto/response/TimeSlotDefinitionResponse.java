package com.tiba.pts.modules.schedule.dto.response;

import java.time.LocalTime;

public record TimeSlotDefinitionResponse(
    Long id,
    String code,
    String label,
    LocalTime startTime,
    LocalTime endTime,
    Integer orderIndex) {}
