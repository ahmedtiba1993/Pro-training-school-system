package com.tiba.pts.modules.execution.dto.response;

import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.domain.enums.SessionType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSessionResponse {
  private Long id;
  private LocalDate sessionDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private SessionType sessionType;
  private SessionStatus status;
  private String lessonTitle;
  private String courseContent;
  private boolean isAttendanceSubmitted;
  private String cancellationReason;
  private Long classGroupId;
  private String className;
  private String specialty;
  private Long subjectId;
  private String subjectName;
  private Long teacherId;
  private String teacherName;
  private Long roomId;
  private String roomName;
  private String formattedDate;
}
