package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtService jwtService;

  @Mock
  private RefreshTokenService refreshTokenService;

  @InjectMocks
  private AuthService authService;

  @Test
  void login_success_shouldReturnAuthDataWithTokenAndUserInfo() {
    AuthRequest request = new AuthRequest("admin_user", "password123");
    User user =
        User.builder()
            .id(1L)
            .username("admin_user")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(false)
            .build();

    RefreshToken refreshToken =
        RefreshToken.builder()
            .token("mock.refresh.token")
            .user(user)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(auth);
    when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");
    when(jwtService.getExpirationInSeconds()).thenReturn(1800L);
    when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

    AuthData result = authService.login(request);

    assertNotNull(result);
    assertEquals("mock.jwt.token", result.getToken());
    assertEquals("mock.refresh.token", result.getRefreshToken());
    assertEquals("Bearer", result.getType());
    assertEquals(1800L, result.getExpiresIn());
    assertNotNull(result.getUser());
    assertEquals("admin_user", result.getUser().getUsername());
    assertEquals("ROLE_ADMIN", result.getUser().getRole());
    assertFalse(result.getUser().isForcePasswordChange());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(jwtService).generateToken(user);
    verify(jwtService).getExpirationInSeconds();
    verify(refreshTokenService).createRefreshToken(user);
  }

  @Test
  void login_shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
    AuthRequest request = new AuthRequest("admin_user", "wrong_password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(BadCredentialsException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
    verify(refreshTokenService, never()).createRefreshToken(any());
  }

  @Test
  void login_shouldThrowLockedException_whenAccountIsSuspended() {
    AuthRequest request = new AuthRequest("suspended_user", "password123");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new LockedException("Account suspended"));

    assertThrows(LockedException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
    verify(refreshTokenService, never()).createRefreshToken(any());
  }

  @Test
  void login_shouldThrowDisabledException_whenAccountIsArchived() {
    AuthRequest request = new AuthRequest("archived_user", "password123");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new DisabledException("Account disabled"));

    assertThrows(DisabledException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
    verify(refreshTokenService, never()).createRefreshToken(any());
  }

  @Test
  void refreshToken_success_shouldReturnNewAccessTokenAndRotatedRefreshToken() {
    TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
    User user =
        User.builder()
            .id(1L)
            .username("admin_user")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    RefreshToken currentToken =
        RefreshToken.builder()
            .token("valid.refresh.token")
            .user(user)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    RefreshToken rotatedToken =
        RefreshToken.builder()
            .token("new.rotated.refresh.token")
            .user(user)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenService.verifyToken("valid.refresh.token")).thenReturn(currentToken);
    when(refreshTokenService.rotateRefreshToken(currentToken)).thenReturn(rotatedToken);
    when(jwtService.generateToken(user)).thenReturn("new.access.token");
    when(jwtService.getExpirationInSeconds()).thenReturn(1800L);

    TokenRefreshResponse response = authService.refreshToken(request);

    assertNotNull(response);
    assertEquals("new.access.token", response.getAccessToken());
    assertEquals("new.rotated.refresh.token", response.getRefreshToken());
    assertEquals("Bearer", response.getTokenType());
    assertEquals(1800L, response.getExpiresIn());

    verify(refreshTokenService).verifyToken("valid.refresh.token");
    verify(refreshTokenService).rotateRefreshToken(currentToken);
    verify(jwtService).generateToken(user);
  }

  @Test
  void refreshToken_shouldThrowLockedException_whenUserIsSuspended() {
    TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
    User user =
        User.builder()
            .id(1L)
            .username("suspended_user")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.SUSPENDED)
            .build();

    RefreshToken currentToken =
        RefreshToken.builder()
            .token("valid.refresh.token")
            .user(user)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenService.verifyToken("valid.refresh.token")).thenReturn(currentToken);

    assertThrows(LockedException.class, () -> authService.refreshToken(request));
    verify(refreshTokenService, never()).rotateRefreshToken(any());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void refreshToken_shouldThrowDisabledException_whenUserIsArchived() {
    TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
    User user =
        User.builder()
            .id(1L)
            .username("archived_user")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ARCHIVED)
            .build();

    RefreshToken currentToken =
        RefreshToken.builder()
            .token("valid.refresh.token")
            .user(user)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenService.verifyToken("valid.refresh.token")).thenReturn(currentToken);

    assertThrows(DisabledException.class, () -> authService.refreshToken(request));
    verify(refreshTokenService, never()).rotateRefreshToken(any());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void logout_shouldCallRefreshTokenServiceRevokeToken() {
    LogoutRequest request = new LogoutRequest("token-to-revoke");
    doNothing().when(refreshTokenService).revokeToken("token-to-revoke");

    authService.logout(request);

    verify(refreshTokenService).revokeToken("token-to-revoke");
  }
}
