package com.tiba.pts.modules.profiles.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.Gender;
import com.tiba.pts.modules.profiles.domain.enums.ParentsSituation;
import com.tiba.pts.modules.profiles.domain.enums.StudentResidence;
import com.tiba.pts.modules.profiles.domain.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

  // --- Infos Person ---
  private Long id;
  private String firstName;
  private String lastName;
  private String firstNameAr;
  private String lastNameAr;
  private String email;
  private String phone;
  private String cin;
  private Gender gender;

  // --- Infos Student ---
  private String studentCode;
  private LocalDate birthDate;
  private String birthPlace;
  private String governorate;
  private String delegation;
  private String residenceAddress;
  private String correspondenceAddress;
  private StudentResidence residence;
  private ParentsSituation parentsSituation;
  private boolean hasChronicDisease;
  private String diseaseDescription;
  private StudentStatus status;

  // --- Associated Details  ---
  private List<StudentSiblingResponse> studentSiblings;
  private List<StudentParentResponse> parents;
  private List<GraduationRecordResponse> graduations;
}
