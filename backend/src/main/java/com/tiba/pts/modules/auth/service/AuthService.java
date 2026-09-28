package com.tiba.pts.modules.auth.service;

import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.UserInfo;
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

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  @Transactional(readOnly = true)
  public AuthData login(AuthRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user = (User) authentication.getPrincipal();
    String token = jwtService.generateToken(user);

    UserInfo userInfo =
        UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole() != null ? user.getRole().name() : null)
            .forcePasswordChange(user.isForcePasswordChange())
            .build();

    return AuthData.builder()
        .token(token)
        .type("Bearer")
        .expiresIn(jwtService.getExpirationInSeconds())
        .user(userInfo)
        .build();
  }
}
