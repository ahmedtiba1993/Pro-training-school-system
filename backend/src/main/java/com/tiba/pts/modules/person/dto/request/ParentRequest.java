package com.tiba.pts.modules.person.dto.request;

import lombok.Data;

@Data
public class ParentRequest {
  private String firstName;
  private String lastName;
  private String email;
  private String phoneNumber;
  private String cin;
  private String profession;
  private String maritalStatus;
  private boolean isDeceased;
  private boolean isLegalGuardian;
}
