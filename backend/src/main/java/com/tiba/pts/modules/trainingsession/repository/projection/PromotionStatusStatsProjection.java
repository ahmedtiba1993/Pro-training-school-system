package com.tiba.pts.modules.trainingsession.repository.projection;

import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;

public interface PromotionStatusStatsProjection {
  PromotionStatus getStatus();

  Long getPromotionCount();

  Long getTotalEnrollments();
}
