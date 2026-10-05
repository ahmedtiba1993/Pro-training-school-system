package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.security.service.AccountStatusValidator;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.repository.RefreshTokenRepository;
import com.tiba.pts.modules.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final RefreshTokenRepository refreshTokenRepository;
  private final AccountStatusValidator accountStatusValidator;

  @Value("${jwt.refresh-token-expiration:2592000}")
  private long refreshTokenExpirationInSeconds;

  @Value("${jwt.revoked-token-retention-days:7}")
  private long revokedTokenRetentionDays;

  public record IssuedRefreshToken(String rawToken, Instant expiryDate) {}

  public record RotatedRefreshToken(User user, IssuedRefreshToken refreshToken) {}

  @Transactional
  public IssuedRefreshToken createRefreshToken(User user) {
    String rawToken = generateRawToken();
    Instant expiryDate = Instant.now().plusSeconds(refreshTokenExpirationInSeconds);

    refreshTokenRepository.save(
        RefreshToken.builder()
            .user(user)
            .token(hash(rawToken))
            .expiryDate(expiryDate)
            .revoked(false)
            .build());

    return new IssuedRefreshToken(rawToken, expiryDate);
  }

  @Transactional(noRollbackFor = BusinessValidationException.class)
  public RotatedRefreshToken rotateRefreshToken(String rawToken) {
    RefreshToken current =
        refreshTokenRepository
            .findByTokenHashWithUser(hash(rawToken))
            .orElseThrow(() -> new ResourceNotFoundException("AUTH_REFRESH_TOKEN_NOT_FOUND"));
    User user = current.getUser();

    if (current.isRevoked()) {
      revokeAllSessionsOnReuse(user);
      throw new BusinessValidationException("AUTH_REFRESH_TOKEN_REVOKED");
    }

    if (current.getExpiryDate().isBefore(Instant.now())) {
      throw new BusinessValidationException("AUTH_REFRESH_TOKEN_EXPIRED");
    }

    accountStatusValidator.validate(user);

    if (refreshTokenRepository.revokeIfActive(current.getId()) == 0) {
      throw new BusinessValidationException("AUTH_REFRESH_TOKEN_REVOKED");
    }

    return new RotatedRefreshToken(user, createRefreshToken(user));
  }

  @Transactional
  public void revokeToken(String rawToken) {
    refreshTokenRepository.revokeByTokenHash(hash(rawToken));
  }

  @Transactional
  public void revokeAllUserTokens(User user) {
    refreshTokenRepository.revokeAllByUser(user);
  }

  @Scheduled(cron = "${jwt.refresh-token-purge-cron:0 0 3 * * *}")
  @Transactional
  public void purgeObsoleteTokens() {
    int deleted =
        refreshTokenRepository.deleteExpiredOrStaleRevokedTokens(
            Instant.now(), LocalDateTime.now().minusDays(revokedTokenRetentionDays));
    log.info("Refresh token purge completed: {} row(s) deleted", deleted);
  }

  private void revokeAllSessionsOnReuse(User user) {
    int revoked = refreshTokenRepository.revokeAllByUser(user);
    log.warn(
        "Refresh token reuse detected for userId={} - {} active session(s) revoked",
        user.getId(),
        revoked);
  }

  private static String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
    SECURE_RANDOM.nextBytes(bytes);
    return TOKEN_ENCODER.encodeToString(bytes);
  }

  static String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
