package com.tiba.pts.modules.profiles.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentListResponse {
  private Long id;
  private String firstName;
  private String lastName;
  private String studentCode;
  private LocalDate birthDate;

  private String studentContact;
  private String guardianContact;

  private StudentStatus status;
}
