package com.tiba.pts.modules.execution.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatsResponse {
  private long totalEffectif;
  private long totalPresents;
  private long totalAbsents;
}
