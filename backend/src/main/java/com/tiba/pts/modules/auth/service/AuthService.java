package com.tiba.pts.modules.auth.service;

import com.tiba.pts.modules.auth.dto.AuthData;
import com.tiba.pts.modules.auth.dto.AuthRequest;
import com.tiba.pts.modules.auth.dto.AuthResponse;
import com.tiba.pts.modules.auth.dto.UserInfo;
import com.tiba.pts.core.security.jwt.JwtService;
import com.tiba.pts.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

  @Value("${jwt.expiration}")
  private Long expiration;

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthResponse login(AuthRequest request) {

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    User user = (User) authentication.getPrincipal();
    String token = jwtService.generateToken(user);

    AuthData authData =
        new AuthData(
            token,
            "Bearer",
            expiration,
            new UserInfo(user.getId(), user.getUsername(), user.getRole().name()));

    return new AuthResponse(true, "Authentication successful", authData, null, LocalDateTime.now());
  }
}
