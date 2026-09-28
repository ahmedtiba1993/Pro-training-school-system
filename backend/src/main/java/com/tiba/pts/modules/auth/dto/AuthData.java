package com.tiba.pts.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthData {
  private String token;
  private String type;
  private long expiresIn;
  private UserInfo user;
}
