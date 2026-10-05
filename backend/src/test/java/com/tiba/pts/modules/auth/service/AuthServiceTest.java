package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.UserInfo;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.auth.mapper.AuthMapper;
import com.tiba.pts.modules.auth.service.RefreshTokenService.IssuedRefreshToken;
import com.tiba.pts.modules.auth.service.RefreshTokenService.RotatedRefreshToken;
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

  @Mock
  private AuthMapper authMapper;

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

    UserInfo userInfo =
        UserInfo.builder()
            .id(1L)
            .username("admin_user")
            .role("ROLE_ADMIN")
            .forcePasswordChange(false)
            .build();

    IssuedRefreshToken issuedRefreshToken =
        new IssuedRefreshToken("mock.raw.refresh.token", Instant.now().plusSeconds(3600));

    Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(auth);
    when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");
    when(jwtService.getExpirationInSeconds()).thenReturn(1800L);
    when(refreshTokenService.createRefreshToken(user)).thenReturn(issuedRefreshToken);
    when(authMapper.toUserInfo(user)).thenReturn(userInfo);

    AuthData result = authService.login(request);

    assertNotNull(result);
    assertEquals("mock.jwt.token", result.getToken());
    assertEquals("mock.raw.refresh.token", result.getRefreshToken());
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
    verify(authMapper).toUserInfo(user);
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
        .thenThrow(new LockedException("AUTH_ACCOUNT_SUSPENDED"));

    assertThrows(LockedException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
    verify(refreshTokenService, never()).createRefreshToken(any());
  }

  @Test
  void login_shouldThrowDisabledException_whenAccountIsArchived() {
    AuthRequest request = new AuthRequest("archived_user", "password123");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new DisabledException("AUTH_ACCOUNT_DISABLED"));

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

    IssuedRefreshToken newIssuedToken =
        new IssuedRefreshToken("new.raw.rotated.token", Instant.now().plusSeconds(3600));
    RotatedRefreshToken rotatedResult = new RotatedRefreshToken(user, newIssuedToken);

    when(refreshTokenService.rotateRefreshToken("valid.refresh.token")).thenReturn(rotatedResult);
    when(jwtService.generateToken(user)).thenReturn("new.access.token");
    when(jwtService.getExpirationInSeconds()).thenReturn(1800L);

    TokenRefreshResponse response = authService.refreshToken(request);

    assertNotNull(response);
    assertEquals("new.access.token", response.getAccessToken());
    assertEquals("new.raw.rotated.token", response.getRefreshToken());
    assertEquals("Bearer", response.getTokenType());
    assertEquals(1800L, response.getExpiresIn());

    verify(refreshTokenService).rotateRefreshToken("valid.refresh.token");
    verify(jwtService).generateToken(user);
    verify(jwtService).getExpirationInSeconds();
  }

  @Test
  void logout_shouldCallRefreshTokenServiceRevokeToken() {
    LogoutRequest request = new LogoutRequest("token-to-revoke");
    doNothing().when(refreshTokenService).revokeToken("token-to-revoke");

    authService.logout(request);

    verify(refreshTokenService).revokeToken("token-to-revoke");
  }
}
