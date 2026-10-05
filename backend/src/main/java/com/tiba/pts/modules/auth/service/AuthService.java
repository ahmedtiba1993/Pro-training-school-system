package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.auth.mapper.AuthMapper;
import com.tiba.pts.modules.auth.service.RefreshTokenService.IssuedRefreshToken;
import com.tiba.pts.modules.auth.service.RefreshTokenService.RotatedRefreshToken;
import com.tiba.pts.modules.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String TOKEN_TYPE = "Bearer";

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final AuthMapper authMapper;

  @Transactional
  public AuthData login(AuthRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username(), request.password()));

    User user = (User) authentication.getPrincipal();
    IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return AuthData.builder()
        .token(jwtService.generateToken(user))
        .refreshToken(refreshToken.rawToken())
        .type(TOKEN_TYPE)
        .expiresIn(jwtService.getExpirationInSeconds())
        .user(authMapper.toUserInfo(user))
        .build();
  }

  @Transactional(noRollbackFor = BusinessValidationException.class)
  public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
    RotatedRefreshToken rotated = refreshTokenService.rotateRefreshToken(request.refreshToken());

    return TokenRefreshResponse.builder()
        .accessToken(jwtService.generateToken(rotated.user()))
        .refreshToken(rotated.refreshToken().rawToken())
        .tokenType(TOKEN_TYPE)
        .expiresIn(jwtService.getExpirationInSeconds())
        .build();
  }

  @Transactional
  public void logout(LogoutRequest request) {
    refreshTokenService.revokeToken(request.refreshToken());
  }
}
