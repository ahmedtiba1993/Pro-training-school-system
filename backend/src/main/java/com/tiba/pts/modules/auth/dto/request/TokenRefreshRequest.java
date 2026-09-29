package com.tiba.pts.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "AUTH_REFRESH_TOKEN_REQUIRED")
    String refreshToken
) {}
