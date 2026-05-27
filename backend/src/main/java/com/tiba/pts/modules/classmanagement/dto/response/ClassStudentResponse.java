package com.tiba.pts.modules.classmanagement.dto.response;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.profiles.domain.enums.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassStudentResponse {
  private String firstName;
  private String lastName;
  private String phone;
  private String studentCode;
  private EnrollmentStatus enrollmentStatus;
  private Gender gender;
  private Long enrollmentId;
}
