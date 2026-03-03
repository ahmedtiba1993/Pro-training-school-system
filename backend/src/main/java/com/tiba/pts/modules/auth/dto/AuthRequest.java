package com.tiba.pts.modules.auth.dto;

import lombok.Data;

@Data
public class AuthRequest {
  private String username;
  private String password;
}
