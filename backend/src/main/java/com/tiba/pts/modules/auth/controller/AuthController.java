package com.tiba.pts.modules.auth.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthData>> login(@Valid @RequestBody AuthRequest request) {
    AuthData authData = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("AUTH_LOGIN_SUCCESS", authData));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
      @Valid @RequestBody TokenRefreshRequest request) {
    TokenRefreshResponse response = authService.refreshToken(request);
    return ResponseEntity.ok(ApiResponse.success("AUTH_TOKEN_REFRESH_SUCCESS", response));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ResponseEntity.ok(ApiResponse.success("AUTH_LOGOUT_SUCCESS", null));
  }
}
