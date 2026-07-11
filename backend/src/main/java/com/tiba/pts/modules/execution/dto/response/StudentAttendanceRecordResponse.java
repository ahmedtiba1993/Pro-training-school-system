package com.tiba.pts.modules.execution.dto.response;

import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceRecordResponse {
  private Long id;
  private LocalDate sessionDate;
  private AttendanceStatus status;
  private LocalTime startTime;
  private LocalTime endTime;
}
