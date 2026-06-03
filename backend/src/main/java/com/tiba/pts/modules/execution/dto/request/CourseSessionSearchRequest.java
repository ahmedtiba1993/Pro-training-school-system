package com.tiba.pts.modules.execution.dto.request;

import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.domain.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSessionSearchRequest {

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  private LocalTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  private LocalTime endTime;

  private Long promotionId;

  private Long teacherId;

  private Long subjectId;

  private SessionStatus status;

  private Boolean isAttendanceSubmitted;

  private SessionType type;
}
