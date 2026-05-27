package com.tiba.pts.modules.enrollment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
public class UnassignedEnrollmentSearchRequest {

  @Setter private Long promotionId;

  private String firstName;
  private String lastName;
  private String cin;
  private String phone;
  private String studentCode;

  // --- Custom setters with Trim + conversion of empty strings to NULL ---

  public void setFirstName(String firstName) {
    this.firstName = clean(firstName);
  }

  public void setLastName(String lastName) {
    this.lastName = clean(lastName);
  }

  public void setCin(String cin) {
    this.cin = clean(cin);
  }

  public void setPhone(String phone) {
    this.phone = clean(phone);
  }

  public void setStudentCode(String studentCode) {
    this.studentCode = clean(studentCode);
  }

  /** Méthode utilitaire interne pour nettoyer la chaîne */
  private String clean(String input) {
    return (input != null && !input.trim().isEmpty()) ? input.trim() : null;
  }
}
