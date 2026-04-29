package com.tiba.pts.modules.profiles.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.ParentsSituation;
import com.tiba.pts.modules.profiles.domain.enums.StudentResidence;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StudentResponse {
  private Long id;
  private String firstName;
  private String lastName;
  private String studentCode;
  private String cin;
  private LocalDate birthDate;
  private StudentResidence residence;
  private ParentsSituation parentsSituation;
  private List<StudentSiblingResponse> studentSiblings;
  private List<StudentParentResponse> parents;
}
