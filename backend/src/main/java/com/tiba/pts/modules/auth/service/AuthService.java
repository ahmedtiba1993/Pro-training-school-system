package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.UserInfo;
import com.tiba.pts.modules.auth.dto.request.LogoutRequest;
import com.tiba.pts.modules.auth.dto.request.TokenRefreshRequest;
import com.tiba.pts.modules.auth.dto.response.TokenRefreshResponse;
import com.tiba.pts.modules.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public AuthData login(AuthRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user = (User) authentication.getPrincipal();
    String token = jwtService.generateToken(user);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    UserInfo userInfo =
        UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole() != null ? user.getRole().name() : null)
            .forcePasswordChange(user.isForcePasswordChange())
            .build();

    return AuthData.builder()
        .token(token)
        .refreshToken(refreshToken.getToken())
        .type("Bearer")
        .expiresIn(jwtService.getExpirationInSeconds())
        .user(userInfo)
        .build();
  }

  @Transactional
  public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
    RefreshToken currentRefreshToken = refreshTokenService.verifyToken(request.refreshToken());

    User user = currentRefreshToken.getUser();
    if (!user.isAccountNonLocked()) {
      throw new LockedException("ACCOUNT_SUSPENDED");
    }
    if (!user.isEnabled()) {
      throw new DisabledException("ACCOUNT_DISABLED");
    }

    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(currentRefreshToken);
    String newAccessToken = jwtService.generateToken(user);

    return TokenRefreshResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken.getToken())
        .tokenType("Bearer")
        .expiresIn(jwtService.getExpirationInSeconds())
        .build();
  }

  @Transactional
  public void logout(LogoutRequest request) {
    refreshTokenService.revokeToken(request.refreshToken());
  }
}
