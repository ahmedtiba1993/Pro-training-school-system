package com.tiba.pts.core.exception;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.ErrorDetail;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials() {
    ApiResponse<Void> response = ApiResponse.error("Invalid credentials", "BAD_CREDENTIALS");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
    ApiResponse<Void> response =
        ApiResponse.error("Access denied: Insufficient permissions", "ERR_ACCESS_DENIED");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(
      io.jsonwebtoken.ExpiredJwtException ex) {
    ApiResponse<Void> response =
        ApiResponse.error("Session expired, please log in again", "TOKEN_EXPIRED");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(io.jsonwebtoken.JwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleJwtException(io.jsonwebtoken.JwtException ex) {
    ApiResponse<Void> response =
        ApiResponse.error("Invalid or corrupted security token", "INVALID_TOKEN");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    List<ErrorDetail> errors = new ArrayList<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String field = ((FieldError) error).getField();
              String message = error.getDefaultMessage();
              errors.add(new ErrorDetail(field, message));
            });

    ApiResponse<Void> response = ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGlobal(Exception ex) {
    log.error("Une erreur interne est survenue : ", ex);
    ApiResponse<Void> response =
        ApiResponse.error("An internal server error occurred", "INTERNAL_SERVER_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleJsonErrors(HttpMessageNotReadableException ex) {

    String rawMessage = ex.getMessage();
    String cleanMessage = "Malformed JSON request";
    if (rawMessage != null && rawMessage.contains(":")) {
      cleanMessage = rawMessage.split(":")[1].trim();

      if (cleanMessage.contains(";")) {
        cleanMessage = cleanMessage.split(";")[0].trim();
      }
    }
    ApiResponse<Void> response = ApiResponse.error(cleanMessage, "ERR_JSON_PARSE");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
    ApiResponse<Void> response =
        ApiResponse.error(ex.getMessage(), "ERR_DUPLICATE_RESOURCE", ex.getConflicts());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(BusinessValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessValidationException(
      BusinessValidationException ex) {
    ApiResponse<Void> response =
        ApiResponse.error(ex.getMessage(), "ERR_VALIDATION", ex.getErrors());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {

    ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), "ERR_NOT_FOUND");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
    ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), "ERR_VALIDATION");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(EntityAlreadyExistsException.class)
  public ResponseEntity<ApiResponse<Void>> handleEntityAlreadyExists(
      EntityAlreadyExistsException ex) {
    ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), "ENTITY_ALREADY_EXISTS");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }
}
