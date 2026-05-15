package com.tiba.pts.modules.user.dto.response;

import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
  private Long id;
  private String username;
  private Role role;
  private UserStatus status;
  private boolean forcePasswordChange;

  // --- Profile Information ---
  private Long personId;
  private String firstName;
  private String lastName;
}
