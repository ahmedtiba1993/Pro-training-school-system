package com.tiba.pts.modules.enrollment.dto.request;

import com.tiba.pts.modules.person.domain.enums.MaritalStatus;
import com.tiba.pts.modules.person.domain.enums.ParentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ParentDto {

  @NotBlank(message = "FIRST_NAME_REQUIRED")
  private String firstName;

  @NotBlank(message = "LAST_NAME_REQUIRED")
  private String lastName;

  @NotBlank(message = "PHONE_NUMBER_REQUIRED")
  private String phoneNumber;

  private String email;
  private LocalDate dateOfBirth;
  private String cin;
  private String profession;
  private ParentType parentType;
  private MaritalStatus maritalStatus;
  private Boolean isDeceased;

  @NotNull(message = "IS_LEGAL_GUARDIAN_REQUIRED")
  private Boolean isLegalGuardian;
}
