package com.tiba.pts.modules.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStatusStatsResponse {
  private long activeCount;
  private long maintenanceCount;
  private long totalOperationalCount;
}
