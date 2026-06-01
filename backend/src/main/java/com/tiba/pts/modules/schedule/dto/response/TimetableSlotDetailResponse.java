package com.tiba.pts.modules.schedule.dto.response;

import com.tiba.pts.modules.schedule.domain.enums.Periodicity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlotDetailResponse {

  private Long id;
  private String subjectName;
  private String teacherName;
  private String roomName;
  private String dayOfWeek;
  private Periodicity periodicity;
  private TimeSlotInfo timeSlot;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeSlotInfo {
    private Long id;
    private String code;
    private String label;
    private String startTime;
    private String endTime;
  }
}
