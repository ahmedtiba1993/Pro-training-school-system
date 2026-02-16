package com.tiba.pts.exception;

import com.tiba.pts.auth.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<AuthResponse> handleBadCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new AuthResponse(false, "Invalid username or password", "AUTH_001"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<AuthResponse> handleAccessDenied() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new AuthResponse(false, "Access denied: insufficient permissions", "AUTH_005"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<AuthResponse> handleGlobal() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new AuthResponse(false, "An internal server error occurred", "SYS_001"));
  }

  @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
  public ResponseEntity<AuthResponse> handleExpiredJwt(io.jsonwebtoken.ExpiredJwtException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new AuthResponse(false, "Token has expired", null, "AUTH_002", LocalDateTime.now()));
  }

  @ExceptionHandler(io.jsonwebtoken.JwtException.class)
  public ResponseEntity<AuthResponse> handleJwtException(io.jsonwebtoken.JwtException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new AuthResponse(false, "Invalid token", null, "AUTH_003", LocalDateTime.now()));
  }
}
