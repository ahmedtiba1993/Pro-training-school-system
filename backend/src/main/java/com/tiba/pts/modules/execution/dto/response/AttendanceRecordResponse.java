package com.tiba.pts.modules.execution.dto.response;

import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordResponse {
  private Long id;
  private Long enrollmentId;
  private String studentName;
  private AttendanceStatus status;
  private String details;
  private String adminNotes;
}
