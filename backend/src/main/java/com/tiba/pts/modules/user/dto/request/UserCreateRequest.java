package com.tiba.pts.modules.user.dto.request;

import com.tiba.pts.modules.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

  @NotBlank(message = "USERNAME_REQUIRED")
  private String username;

  @NotBlank(message = "PASSWORD_REQUIRED")
  private String password;

  @NotNull(message = "ROLE_REQUIRED")
  private Role role;

  private Long personId;
}
