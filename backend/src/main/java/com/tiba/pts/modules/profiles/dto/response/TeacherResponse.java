package com.tiba.pts.modules.profiles.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.ContractType;
import com.tiba.pts.modules.profiles.domain.enums.AcademicDegree;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.domain.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class TeacherResponse {
  // Person fields
  private Long id;
  private String firstName;
  private String lastName;
  private String firstNameAr;
  private String lastNameAr;
  private String email;
  private String phone;
  private String cin;
  private Gender gender;

  // Teacher fields
  private String code;
  private ContractType contractType;
  private LocalDate hireDate;
  private TeacherStatus status;
  private AcademicDegree degree;

  // Associated specialties
  private Set<RefTeacherSpecialtySimpleResponse> specialties;
}
