package com.tiba.pts.modules.trainingsession.dto.response;

import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;

public record AccreditedPromotionStatsResponse(
    PromotionStatus status, Long promotionCount, Long totalEnrollments) {}
