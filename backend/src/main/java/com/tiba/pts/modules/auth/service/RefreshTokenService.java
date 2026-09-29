package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.repository.RefreshTokenRepository;
import com.tiba.pts.modules.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh-token-expiration:2592000}")
  private Long refreshTokenExpirationInSeconds;

  @Transactional
  public RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiryDate(Instant.now().plusSeconds(refreshTokenExpirationInSeconds))
            .revoked(false)
            .build();

    return refreshTokenRepository.save(refreshToken);
  }

  @Transactional(readOnly = true)
  public RefreshToken verifyToken(String token) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenWithUser(token)
            .orElseThrow(() -> new ResourceNotFoundException("AUTH_REFRESH_TOKEN_NOT_FOUND"));

    if (refreshToken.isRevoked()) {
      throw new BusinessValidationException("AUTH_REFRESH_TOKEN_REVOKED");
    }

    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
      throw new BusinessValidationException("AUTH_REFRESH_TOKEN_EXPIRED");
    }

    return refreshToken;
  }

  @Transactional
  public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
    oldToken.setRevoked(true);
    refreshTokenRepository.save(oldToken);
    return createRefreshToken(oldToken.getUser());
  }

  @Transactional
  public void revokeToken(String token) {
    refreshTokenRepository
        .findByToken(token)
        .ifPresent(
            rt -> {
              rt.setRevoked(true);
              refreshTokenRepository.save(rt);
            });
  }

  @Transactional
  public void revokeAllUserTokens(User user) {
    refreshTokenRepository.revokeAllByUser(user);
  }
}
