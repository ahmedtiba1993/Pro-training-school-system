package com.tiba.pts.core.security.jwt;

import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

  private JwtService jwtService;
  private final String testSecret = "0619Oaz0w+CrG8qQc36UHreZXI6qXs6zBZgL/LXJVGI=";

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secret", testSecret);
    ReflectionTestUtils.setField(jwtService, "expirationInSeconds", 3600L);
    jwtService.init();
  }

  @Test
  void generateToken_shouldCreateValidTokenWithCorrectSubjectAndRole() {
    User user =
        User.builder()
            .id(1L)
            .username("admin_test")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    String token = jwtService.generateToken(user);

    assertNotNull(token);
    assertFalse(token.isBlank());
    assertEquals("admin_test", jwtService.extractUsername(token));
    assertTrue(jwtService.isTokenValid(token, user));
  }

  @Test
  void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
    User user =
        User.builder()
            .id(1L)
            .username("user_one")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    User anotherUser =
        User.builder()
            .id(2L)
            .username("user_two")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    String token = jwtService.generateToken(user);

    assertFalse(jwtService.isTokenValid(token, anotherUser));
  }

  @Test
  void isTokenValid_shouldReturnFalse_whenUserIsSuspended() {
    User user =
        User.builder()
            .id(1L)
            .username("suspended_user")
            .role(Role.ROLE_STUDENT)
            .status(UserStatus.SUSPENDED)
            .build();

    String token = jwtService.generateToken(user);

    assertFalse(jwtService.isTokenValid(token, user));
  }

  @Test
  void isTokenValid_shouldReturnFalse_whenUserIsArchived() {
    User user =
        User.builder()
            .id(1L)
            .username("archived_user")
            .role(Role.ROLE_STUDENT)
            .status(UserStatus.ARCHIVED)
            .build();

    String token = jwtService.generateToken(user);

    assertFalse(jwtService.isTokenValid(token, user));
  }
}
