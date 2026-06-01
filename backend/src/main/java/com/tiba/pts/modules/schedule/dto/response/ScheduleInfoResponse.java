package com.tiba.pts.modules.schedule.dto.response;

import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleInfoResponse {
  private Long id;
  private String label;
  private ScheduleStatus status;
  private Long promotionId;
}
