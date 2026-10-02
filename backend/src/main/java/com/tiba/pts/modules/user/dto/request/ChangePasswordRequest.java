package com.tiba.pts.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "OLD_PASSWORD_REQUIRED")
    String oldPassword,

    @NotBlank(message = "NEW_PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    String newPassword,

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    String confirmNewPassword
) {
  @JsonIgnore
  @AssertTrue(message = "PASSWORDS_DO_NOT_MATCH")
  public boolean isPasswordsMatching() {
    if (newPassword == null || confirmNewPassword == null) {
      return false;
    }
    return newPassword.equals(confirmNewPassword);
  }
}
