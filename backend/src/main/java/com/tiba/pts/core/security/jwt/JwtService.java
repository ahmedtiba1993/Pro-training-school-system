package com.tiba.pts.core.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
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
  @Value("${jwt.expiration:1800}")
  private Long expirationInSeconds;

  private SecretKey key;
  private JwtParser jwtParser;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.jwtParser = Jwts.parser().verifyWith(this.key).build();
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
    Claims claims = extractClaims(token);
    String username = claims.getSubject();
    boolean isExpired = claims.getExpiration().before(new Date());

    return username.equals(user.getUsername())
        && !isExpired
        && user.isEnabled()
        && user.isAccountNonLocked();
  }

  public boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  public Claims extractClaims(String token) {
    return jwtParser.parseSignedClaims(token).getPayload();
  }
}
