package com.tiba.pts.modules.enrollment.dto.response;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnrollmentResponse {

  private Long id;

  private String studentFullName;
  private LocalDate dateOfBirth;
  private String studentPhoneNumber;
  private String guardianPhoneNumber;

  private String training;
  private String promotionName;

  private LocalDateTime enrollmentDate;
  private EnrollmentStatus status;
}
