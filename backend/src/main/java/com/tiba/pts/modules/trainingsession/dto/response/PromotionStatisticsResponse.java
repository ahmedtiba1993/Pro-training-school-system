package com.tiba.pts.modules.trainingsession.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionStatisticsResponse {

  private long activeSessionsCount;
  private long plannedSessionsCount;
  private long closedSessionsCount;
  private long activeLearnersCount;
}
