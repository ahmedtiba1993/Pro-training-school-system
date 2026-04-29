package com.tiba.pts.modules.enrollment.dto.response;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnrollmentListResponse {
  private Long id;

  // Student Info
  private String studentFirstName;
  private String studentLastName;
  private LocalDate studentBirthDate;
  private String studentPhone;
  private String guardianPhone;

  // Promotion & Training Info
  private String promotionName;
  private String levelLabel;
  private String specialityLabel;

  // Enrollment Info
  private LocalDateTime createdDate;
  private EnrollmentStatus status;
}
