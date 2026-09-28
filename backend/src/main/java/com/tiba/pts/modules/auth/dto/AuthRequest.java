package com.tiba.pts.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank(message = "AUTH_USERNAME_REQUIRED")
    @Size(max = 50, message = "AUTH_USERNAME_INVALID_LENGTH")
    String username,

    @NotBlank(message = "AUTH_PASSWORD_REQUIRED")
    @Size(max = 128, message = "AUTH_PASSWORD_INVALID_LENGTH")
    String password
) {}
