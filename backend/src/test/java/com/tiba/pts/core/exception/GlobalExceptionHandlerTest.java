package com.tiba.pts.core.exception;

import com.tiba.pts.core.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  void handleAccessDenied_whenAuthForcePasswordChange_shouldReturn403WithSpecificErrorCode() {
    AccessDeniedException ex = new AccessDeniedException("AUTH_FORCE_PASSWORD_CHANGE");

    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals("AUTH_FORCE_PASSWORD_CHANGE", response.getBody().getErrorCode());
    assertEquals(
        "Password change is required before accessing any resource",
        response.getBody().getMessage());
  }

  @Test
  void handleAccessDenied_whenGenericAccessDenied_shouldReturn403WithGenericErrorCode() {
    AccessDeniedException ex = new AccessDeniedException("ERR_ACCESS_DENIED");

    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals("ERR_ACCESS_DENIED", response.getBody().getErrorCode());
    assertEquals(
        "Access denied: Insufficient permissions",
        response.getBody().getMessage());
  }
}
