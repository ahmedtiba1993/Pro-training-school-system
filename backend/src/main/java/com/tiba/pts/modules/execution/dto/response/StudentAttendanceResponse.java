package com.tiba.pts.modules.execution.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceResponse {
  private Long enrollmentId;
  private String firstName;
  private String lastName;
  private String studentCode;
  private String status;
}
