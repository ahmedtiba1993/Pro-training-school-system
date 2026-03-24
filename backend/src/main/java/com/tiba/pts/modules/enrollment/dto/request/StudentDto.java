package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentDto {

  @NotBlank(message = "FIRST_NAME_REQUIRED")
  private String firstName;

  @NotBlank(message = "LAST_NAME_REQUIRED")
  private String lastName;

  private String email;

  @NotBlank(message = "PHONE_NUMBERR_EQUIRED")
  private String phoneNumber;

  @NotNull(message = "DATE_BIRTH_REQUIRED")
  private LocalDate dateOfBirth;

  private String cin;

  @NotBlank(message = "ADDRESS_REQUIRED")
  private String address;

  @NotBlank(message = "EDUCATION_LEVEL_REQUIRED")
  private String educationLevel;
}
