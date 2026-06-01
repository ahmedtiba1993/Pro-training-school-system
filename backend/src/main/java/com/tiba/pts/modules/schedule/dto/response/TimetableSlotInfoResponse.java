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
public class TimetableSlotInfoResponse {

  private Long id;
  private String dayOfWeek;
  private Periodicity periodicity;
  private TimeSlotDefId timeSlotDefinition;
  private SubjectInfo subject;
  private TeacherInfo teacher;
  private RoomInfo room;

  // Nested classes to match your JSON exactly

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeSlotDefId {
    private Long id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SubjectInfo {
    private Long id;
    private String name;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeacherInfo {
    private Long id;
    private String name;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomInfo {
    private Long id;
    private String name;
  }
}
