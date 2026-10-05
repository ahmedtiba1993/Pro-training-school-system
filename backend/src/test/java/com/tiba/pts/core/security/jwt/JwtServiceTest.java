package com.tiba.pts.core.security.jwt;

import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

  private JwtService jwtService;
  private final String testSecret = "0619Oaz0w+CrG8qQc36UHreZXI6qXs6zBZgL/LXJVGI=";
  private final String testIssuer = "pts-backend";
  private final long testExpiration = 3600L;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(testSecret, testExpiration, testIssuer);
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

    Claims claims = jwtService.parseAndValidate(token);
    assertEquals("admin_test", claims.getSubject());
    assertEquals("ROLE_ADMIN", claims.get("role", String.class));
    assertEquals(testIssuer, claims.getIssuer());
    assertNotNull(claims.getId());
  }

  @Test
  void parseAndValidate_shouldThrowMalformedJwtException_whenTokenIsNull() {
    assertThrows(MalformedJwtException.class, () -> jwtService.parseAndValidate(null));
  }

  @Test
  void parseAndValidate_shouldThrowMalformedJwtException_whenTokenIsBlank() {
    assertThrows(MalformedJwtException.class, () -> jwtService.parseAndValidate("   "));
  }

  @Test
  void parseAndValidate_shouldThrowException_whenIssuerIsInvalid() {
    JwtService anotherIssuerService = new JwtService(testSecret, testExpiration, "different-issuer");
    User user =
        User.builder()
            .id(1L)
            .username("admin_test")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();
    String tokenFromDifferentIssuer = anotherIssuerService.generateToken(user);

    assertThrows(Exception.class, () -> jwtService.parseAndValidate(tokenFromDifferentIssuer));
  }

  @Test
  void constructor_shouldFailFast_whenKeyIsWeak() {
    String weakSecret = "dG9vLXNob3J0"; // < 256 bits
    assertThrows(WeakKeyException.class, () -> new JwtService(weakSecret, testExpiration, testIssuer));
  }

  @Test
  void constructor_shouldFailFast_whenExpirationIsZeroOrNegative() {
    assertThrows(IllegalStateException.class, () -> new JwtService(testSecret, 0L, testIssuer));
    assertThrows(IllegalStateException.class, () -> new JwtService(testSecret, -10L, testIssuer));
  }
}
