package com.tiba.pts.modules.trainingsession.dto.response;

import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;

import java.time.LocalDate;

public record OngoingPromotionResponse(
    Long id,
    String code,
    String name,
    Integer enrollmentCount,
    Integer capacity,
    PromotionStatus status,
    LocalDate registrationOpeningDate,
    LocalDate registrationDeadline) {}
