package com.tiba.pts.modules.execution.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSessionStatsResponse {
  private long activeSessionsCount;
  private long pendingAttendanceCount;
  private long canceledSessionsCount;
}
