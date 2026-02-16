package com.tiba.pts.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    boolean success, String message, Object data, String errorCode, LocalDateTime timestamp) {

  public AuthResponse(boolean success, String message, String errorCode) {
    this(success, message, null, errorCode, LocalDateTime.now());
  }

  public AuthResponse(boolean success, String message, Object data, String errorCode) {
    this(success, message, data, errorCode, LocalDateTime.now());
  }
}
