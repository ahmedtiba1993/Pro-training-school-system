package com.tiba.pts.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.GlobalExceptionHandler;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.UserInfo;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void login_success_shouldReturn200AndApiResponse() throws Exception {
    AuthRequest request = new AuthRequest("admin_user", "password123");
    AuthData authData =
        AuthData.builder()
            .token("valid.jwt.token")
            .refreshToken("valid.refresh.token")
            .type("Bearer")
            .expiresIn(1800L)
            .user(
                UserInfo.builder()
                    .id(1L)
                    .username("admin_user")
                    .role("ROLE_ADMIN")
                    .forcePasswordChange(false)
                    .build())
            .build();

    when(authService.login(any(AuthRequest.class))).thenReturn(authData);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("AUTH_LOGIN_SUCCESS"))
        .andExpect(jsonPath("$.data.token").value("valid.jwt.token"))
        .andExpect(jsonPath("$.data.refreshToken").value("valid.refresh.token"))
        .andExpect(jsonPath("$.data.type").value("Bearer"))
        .andExpect(jsonPath("$.data.expiresIn").value(1800))
        .andExpect(jsonPath("$.data.user.username").value("admin_user"));
  }

  @Test
  void login_blankUsername_shouldReturn400BadRequest() throws Exception {
    AuthRequest request = new AuthRequest("", "password123");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void login_blankPassword_shouldReturn400BadRequest() throws Exception {
    AuthRequest request = new AuthRequest("admin_user", "");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void login_oversizedPassword_shouldReturn400BadRequest() throws Exception {
    String longPassword = "A".repeat(129);
    AuthRequest request = new AuthRequest("admin_user", longPassword);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void login_badCredentials_shouldReturn401Unauthorized() throws Exception {
    AuthRequest request = new AuthRequest("admin_user", "wrong_password");
    when(authService.login(any(AuthRequest.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("BAD_CREDENTIALS"));
  }

  @Test
  void login_accountSuspended_shouldReturn401Unauthorized() throws Exception {
    AuthRequest request = new AuthRequest("suspended_user", "password123");
    when(authService.login(any(AuthRequest.class)))
        .thenThrow(new LockedException("Account suspended"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_SUSPENDED"));
  }

  @Test
  void login_accountDisabled_shouldReturn401Unauthorized() throws Exception {
    AuthRequest request = new AuthRequest("disabled_user", "password123");
    when(authService.login(any(AuthRequest.class)))
        .thenThrow(new DisabledException("Account disabled"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
  }

  @Test
  void refreshToken_success_shouldReturn200AndApiResponse() throws Exception {
    TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
    TokenRefreshResponse response =
        TokenRefreshResponse.builder()
            .accessToken("new.access.token")
            .refreshToken("new.refresh.token")
            .tokenType("Bearer")
            .expiresIn(1800L)
            .build();

    when(authService.refreshToken(any(TokenRefreshRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("AUTH_TOKEN_REFRESH_SUCCESS"))
        .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
        .andExpect(jsonPath("$.data.refreshToken").value("new.refresh.token"))
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.data.expiresIn").value(1800));
  }

  @Test
  void refreshToken_blankToken_shouldReturn400BadRequest() throws Exception {
    TokenRefreshRequest request = new TokenRefreshRequest("");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void refreshToken_tokenExpired_shouldReturn409Conflict() throws Exception {
    TokenRefreshRequest request = new TokenRefreshRequest("expired.refresh.token");
    when(authService.refreshToken(any(TokenRefreshRequest.class)))
        .thenThrow(new BusinessValidationException("AUTH_REFRESH_TOKEN_EXPIRED"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
        .andExpect(jsonPath("$.message").value("AUTH_REFRESH_TOKEN_EXPIRED"));
  }

  @Test
  void refreshToken_tokenRevoked_shouldReturn409Conflict() throws Exception {
    TokenRefreshRequest request = new TokenRefreshRequest("revoked.refresh.token");
    when(authService.refreshToken(any(TokenRefreshRequest.class)))
        .thenThrow(new BusinessValidationException("AUTH_REFRESH_TOKEN_REVOKED"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
        .andExpect(jsonPath("$.message").value("AUTH_REFRESH_TOKEN_REVOKED"));
  }

  @Test
  void refreshToken_tokenNotFound_shouldReturn404NotFound() throws Exception {
    TokenRefreshRequest request = new TokenRefreshRequest("unknown.refresh.token");
    when(authService.refreshToken(any(TokenRefreshRequest.class)))
        .thenThrow(new ResourceNotFoundException("AUTH_REFRESH_TOKEN_NOT_FOUND"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("AUTH_REFRESH_TOKEN_NOT_FOUND"));
  }

  @Test
  void logout_success_shouldReturn200AndApiResponse() throws Exception {
    LogoutRequest request = new LogoutRequest("token.to.revoke");
    doNothing().when(authService).logout(any(LogoutRequest.class));

    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("AUTH_LOGOUT_SUCCESS"));

    verify(authService).logout(any(LogoutRequest.class));
  }

  @Test
  void logout_blankToken_shouldReturn400BadRequest() throws Exception {
    LogoutRequest request = new LogoutRequest("");

    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }
}
