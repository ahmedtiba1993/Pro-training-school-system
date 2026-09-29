package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.repository.RefreshTokenRepository;
import com.tiba.pts.modules.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private RefreshTokenService refreshTokenService;

  private User mockUser;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationInSeconds", 2592000L);
    mockUser = User.builder().id(1L).username("test_user").build();
  }

  @Test
  void createRefreshToken_success_shouldGenerateAndSaveToken() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshToken created = refreshTokenService.createRefreshToken(mockUser);

    assertNotNull(created);
    assertNotNull(created.getToken());
    assertEquals(mockUser, created.getUser());
    assertFalse(created.isRevoked());
    assertTrue(created.getExpiryDate().isAfter(Instant.now()));

    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void verifyToken_success_shouldReturnRefreshToken() {
    String tokenStr = "valid-token-uuid";
    RefreshToken token =
        RefreshToken.builder()
            .token(tokenStr)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenWithUser(tokenStr)).thenReturn(Optional.of(token));

    RefreshToken result = refreshTokenService.verifyToken(tokenStr);

    assertNotNull(result);
    assertEquals(tokenStr, result.getToken());
    verify(refreshTokenRepository).findByTokenWithUser(tokenStr);
  }

  @Test
  void verifyToken_notFound_shouldThrowResourceNotFoundException() {
    String tokenStr = "non-existing-token";
    when(refreshTokenRepository.findByTokenWithUser(tokenStr)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> refreshTokenService.verifyToken(tokenStr));

    assertEquals("AUTH_REFRESH_TOKEN_NOT_FOUND", exception.getMessage());
  }

  @Test
  void verifyToken_revoked_shouldThrowBusinessValidationException() {
    String tokenStr = "revoked-token";
    RefreshToken token =
        RefreshToken.builder()
            .token(tokenStr)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(true)
            .build();

    when(refreshTokenRepository.findByTokenWithUser(tokenStr)).thenReturn(Optional.of(token));

    BusinessValidationException exception =
        assertThrows(
            BusinessValidationException.class, () -> refreshTokenService.verifyToken(tokenStr));

    assertEquals("AUTH_REFRESH_TOKEN_REVOKED", exception.getMessage());
  }

  @Test
  void verifyToken_expired_shouldThrowBusinessValidationException() {
    String tokenStr = "expired-token";
    RefreshToken token =
        RefreshToken.builder()
            .token(tokenStr)
            .user(mockUser)
            .expiryDate(Instant.now().minus(1, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenWithUser(tokenStr)).thenReturn(Optional.of(token));

    BusinessValidationException exception =
        assertThrows(
            BusinessValidationException.class, () -> refreshTokenService.verifyToken(tokenStr));

    assertEquals("AUTH_REFRESH_TOKEN_EXPIRED", exception.getMessage());
  }

  @Test
  void rotateRefreshToken_shouldRevokeOldAndCreateNew() {
    RefreshToken oldToken =
        RefreshToken.builder()
            .token("old-token-uuid")
            .user(mockUser)
            .expiryDate(Instant.now().plus(20, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldToken);

    assertTrue(oldToken.isRevoked());
    assertNotNull(newToken);
    assertNotEquals("old-token-uuid", newToken.getToken());
    assertEquals(mockUser, newToken.getUser());
    assertFalse(newToken.isRevoked());

    verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void revokeToken_shouldMarkTokenAsRevokedWhenFound() {
    String tokenStr = "token-to-revoke";
    RefreshToken token =
        RefreshToken.builder()
            .token(tokenStr)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    refreshTokenService.revokeToken(tokenStr);

    assertTrue(token.isRevoked());
    verify(refreshTokenRepository).findByToken(tokenStr);
    verify(refreshTokenRepository).save(token);
  }

  @Test
  void revokeAllUserTokens_shouldCallRepository() {
    when(refreshTokenRepository.revokeAllByUser(mockUser)).thenReturn(3);

    refreshTokenService.revokeAllUserTokens(mockUser);

    verify(refreshTokenRepository).revokeAllByUser(mockUser);
  }
}
