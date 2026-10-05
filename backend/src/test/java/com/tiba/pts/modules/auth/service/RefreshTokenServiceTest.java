package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.security.service.AccountStatusValidator;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.repository.RefreshTokenRepository;
import com.tiba.pts.modules.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private AccountStatusValidator accountStatusValidator;

  @InjectMocks
  private RefreshTokenService refreshTokenService;

  private User mockUser;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationInSeconds", 2592000L);
    ReflectionTestUtils.setField(refreshTokenService, "revokedTokenRetentionDays", 7L);
    mockUser = User.builder().id(1L).username("test_user").build();
  }

  @Test
  void createRefreshToken_success_shouldGenerateHashedTokenAndReturnRawToken() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.createRefreshToken(mockUser);

    assertNotNull(issued);
    assertNotNull(issued.rawToken());
    assertFalse(issued.rawToken().isBlank());
    assertTrue(issued.expiryDate().isAfter(Instant.now()));

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());

    RefreshToken saved = captor.getValue();
    assertEquals(RefreshTokenService.hash(issued.rawToken()), saved.getToken());
    assertEquals(mockUser, saved.getUser());
    assertFalse(saved.isRevoked());
  }

  @Test
  void rotateRefreshToken_success_shouldRevokeOldAndReturnNewIssuedToken() {
    String rawToken = "raw-refresh-token-12345";
    String tokenHash = RefreshTokenService.hash(rawToken);

    RefreshToken currentToken =
        RefreshToken.builder()
            .id(10L)
            .token(tokenHash)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenHashWithUser(tokenHash))
        .thenReturn(Optional.of(currentToken));
    when(refreshTokenRepository.revokeIfActive(10L)).thenReturn(1);
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenService.RotatedRefreshToken result = refreshTokenService.rotateRefreshToken(rawToken);

    assertNotNull(result);
    assertEquals(mockUser, result.user());
    assertNotNull(result.refreshToken().rawToken());
    assertNotEquals(rawToken, result.refreshToken().rawToken());

    verify(accountStatusValidator).validate(mockUser);
    verify(refreshTokenRepository).revokeIfActive(10L);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void rotateRefreshToken_notFound_shouldThrowResourceNotFoundException() {
    String rawToken = "unknown-token";
    String tokenHash = RefreshTokenService.hash(rawToken);
    when(refreshTokenRepository.findByTokenHashWithUser(tokenHash)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> refreshTokenService.rotateRefreshToken(rawToken));

    assertEquals("AUTH_REFRESH_TOKEN_NOT_FOUND", exception.getMessage());
  }

  @Test
  void rotateRefreshToken_revoked_shouldRevokeAllSessionsOnReuseAndThrowException() {
    String rawToken = "replayed-revoked-token";
    String tokenHash = RefreshTokenService.hash(rawToken);

    RefreshToken currentToken =
        RefreshToken.builder()
            .id(10L)
            .token(tokenHash)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(true)
            .build();

    when(refreshTokenRepository.findByTokenHashWithUser(tokenHash))
        .thenReturn(Optional.of(currentToken));

    BusinessValidationException exception =
        assertThrows(
            BusinessValidationException.class, () -> refreshTokenService.rotateRefreshToken(rawToken));

    assertEquals("AUTH_REFRESH_TOKEN_REVOKED", exception.getMessage());
    verify(refreshTokenRepository).revokeAllByUser(mockUser);
    verify(refreshTokenRepository, never()).revokeIfActive(any());
  }

  @Test
  void rotateRefreshToken_expired_shouldThrowBusinessValidationException() {
    String rawToken = "expired-token";
    String tokenHash = RefreshTokenService.hash(rawToken);

    RefreshToken currentToken =
        RefreshToken.builder()
            .id(10L)
            .token(tokenHash)
            .user(mockUser)
            .expiryDate(Instant.now().minus(1, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenHashWithUser(tokenHash))
        .thenReturn(Optional.of(currentToken));

    BusinessValidationException exception =
        assertThrows(
            BusinessValidationException.class, () -> refreshTokenService.rotateRefreshToken(rawToken));

    assertEquals("AUTH_REFRESH_TOKEN_EXPIRED", exception.getMessage());
    verify(refreshTokenRepository, never()).revokeAllByUser(any());
    verify(refreshTokenRepository, never()).revokeIfActive(any());
  }

  @Test
  void rotateRefreshToken_concurrencyLost_shouldThrowBusinessValidationExceptionWithoutRevokingAll() {
    String rawToken = "concurrent-token";
    String tokenHash = RefreshTokenService.hash(rawToken);

    RefreshToken currentToken =
        RefreshToken.builder()
            .id(10L)
            .token(tokenHash)
            .user(mockUser)
            .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenHashWithUser(tokenHash))
        .thenReturn(Optional.of(currentToken));
    when(refreshTokenRepository.revokeIfActive(10L)).thenReturn(0);

    BusinessValidationException exception =
        assertThrows(
            BusinessValidationException.class, () -> refreshTokenService.rotateRefreshToken(rawToken));

    assertEquals("AUTH_REFRESH_TOKEN_REVOKED", exception.getMessage());
    verify(refreshTokenRepository, never()).revokeAllByUser(any());
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void revokeToken_shouldCallRepositoryWithHashedToken() {
    String rawToken = "token-to-revoke";
    String expectedHash = RefreshTokenService.hash(rawToken);

    refreshTokenService.revokeToken(rawToken);

    verify(refreshTokenRepository).revokeByTokenHash(expectedHash);
  }

  @Test
  void revokeAllUserTokens_shouldCallRepository() {
    when(refreshTokenRepository.revokeAllByUser(mockUser)).thenReturn(2);

    refreshTokenService.revokeAllUserTokens(mockUser);

    verify(refreshTokenRepository).revokeAllByUser(mockUser);
  }

  @Test
  void purgeObsoleteTokens_shouldCallRepository() {
    when(refreshTokenRepository.deleteExpiredOrStaleRevokedTokens(any(), any())).thenReturn(5);

    refreshTokenService.purgeObsoleteTokens();

    verify(refreshTokenRepository).deleteExpiredOrStaleRevokedTokens(any(), any());
  }
}
