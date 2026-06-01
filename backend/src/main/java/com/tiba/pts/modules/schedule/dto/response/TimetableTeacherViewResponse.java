package com.tiba.pts.modules.schedule.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableTeacherViewResponse {
  private Long teacherId;
  private String teacherName;
  private List<TimeSlotDefinitionResponse> timeSlotDefinitions;
  private List<TimetableSlotInfoResponse> timetableSlots;
}
