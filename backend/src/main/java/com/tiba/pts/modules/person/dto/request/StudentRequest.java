package com.tiba.pts.modules.person.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentRequest {
  private String firstName;
  private String lastName;
  private String email;
  private String phoneNumber;
  private LocalDate dateOfBirth;
  private String cin;
  private String address;
  private String educationLevel;
}
