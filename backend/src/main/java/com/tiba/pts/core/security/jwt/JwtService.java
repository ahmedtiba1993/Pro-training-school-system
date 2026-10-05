package com.tiba.pts.core.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

  private static final String ROLE_CLAIM = "role";
  private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30L;

  private final SecretKey signingKey;
  private final JwtParser jwtParser;
  private final String issuer;
  @Getter private final long expirationInSeconds;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration:1800}") long expirationInSeconds,
      @Value("${jwt.issuer:pts-backend}") String issuer) {
    if (expirationInSeconds <= 0) {
      throw new IllegalStateException("jwt.expiration must be strictly positive");
    }
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.expirationInSeconds = expirationInSeconds;
    this.issuer = issuer;
    this.jwtParser =
        Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
            .build();
  }

  public String generateToken(UserDetails user) {
    String role =
        user.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("");

    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .issuer(issuer)
        .subject(user.getUsername())
        .claim(ROLE_CLAIM, role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationInSeconds)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Parse et valide la signature, l'expiration et l'issuer en une seule operation.
   *
   * @throws io.jsonwebtoken.JwtException si le token est invalide ou expire
   */
  public Claims parseAndValidate(String token) {
    if (token == null || token.isBlank()) {
      throw new MalformedJwtException("AUTH_TOKEN_MISSING");
    }
    return jwtParser.parseSignedClaims(token).getPayload();
  }
}
