package com.tiba.pts.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChangePasswordRequest {

  @NotBlank(message = "PASSWORD_REQUIRED")
  @Size(min = 6, message = "PASSWORD_TOO_SHORT")
  private String newPassword;

  @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
  private String confirmNewPassword;

  // --- Vérification directe dans le DTO ---
  @JsonIgnore
  @AssertTrue(message = "PASSWORDS_DO_NOT_MATCH")
  public boolean isPasswordsMatching() {
    if (newPassword == null || confirmNewPassword == null) {
      return false;
    }
    return newPassword.equals(confirmNewPassword);
  }
}
