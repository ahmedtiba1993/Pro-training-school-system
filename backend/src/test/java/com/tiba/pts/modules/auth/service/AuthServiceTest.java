package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtService jwtService;

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

    Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(auth);
    when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");
    when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

    AuthData result = authService.login(request);

    assertNotNull(result);
    assertEquals("mock.jwt.token", result.getToken());
    assertEquals("Bearer", result.getType());
    assertEquals(3600L, result.getExpiresIn());
    assertNotNull(result.getUser());
    assertEquals("admin_user", result.getUser().getUsername());
    assertEquals("ROLE_ADMIN", result.getUser().getRole());
    assertFalse(result.getUser().isForcePasswordChange());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(jwtService).generateToken(user);
    verify(jwtService).getExpirationInSeconds();
  }

  @Test
  void login_shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
    AuthRequest request = new AuthRequest("admin_user", "wrong_password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(BadCredentialsException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void login_shouldThrowLockedException_whenAccountIsSuspended() {
    AuthRequest request = new AuthRequest("suspended_user", "password123");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new LockedException("Account suspended"));

    assertThrows(LockedException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void login_shouldThrowDisabledException_whenAccountIsArchived() {
    AuthRequest request = new AuthRequest("archived_user", "password123");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new DisabledException("Account disabled"));

    assertThrows(DisabledException.class, () -> authService.login(request));
    verify(jwtService, never()).generateToken(any());
  }
}
