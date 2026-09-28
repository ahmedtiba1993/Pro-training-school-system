package com.tiba.pts.core.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;

  @Getter
  @Value("${jwt.expiration:3600}")
  private Long expirationInSeconds;

  private SecretKey key;

  @PostConstruct
  public void init() {
    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String generateToken(UserDetails user) {
    String role =
        user.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("");

    long now = System.currentTimeMillis();
    long expirationMillis = expirationInSeconds * 1000L;

    return Jwts.builder()
        .subject(user.getUsername())
        .claim("role", role)
        .issuedAt(new Date(now))
        .expiration(new Date(now + expirationMillis))
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return extractClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserDetails user) {
    String username = extractUsername(token);
    return username.equals(user.getUsername())
        && !isTokenExpired(token)
        && user.isEnabled()
        && user.isAccountNonLocked();
  }

  public boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  private Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
