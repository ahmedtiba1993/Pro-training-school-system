package com.tiba.pts.core.security.filter;

import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForcePasswordChangeFilterTest {

  @Mock
  private HandlerExceptionResolver resolver;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Mock
  private Authentication authentication;

  @Mock
  private SecurityContext securityContext;

  private ForcePasswordChangeFilter filter;

  @BeforeEach
  void setUp() {
    filter = new ForcePasswordChangeFilter(resolver);
    lenient().when(request.getMethod()).thenReturn("GET");
    lenient().when(request.getContextPath()).thenReturn("");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(User user) {
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(user);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void doFilterInternal_whenOptionsMethod_shouldContinueFilterChain() throws Exception {
    when(request.getMethod()).thenReturn("OPTIONS");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenNotAuthenticated_shouldContinueFilterChain() throws Exception {
    SecurityContextHolder.clearContext();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeFalse_shouldContinueFilterChain() throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(false)
            .build();

    setAuthenticatedUser(user);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsChangePasswordV1_shouldContinue()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/api/v1/users/change-password");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsChangePasswordWithoutV1_shouldContinue()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/api/users/change-password");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsLogout_shouldContinue()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/api/auth/logout");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsSwagger_shouldContinue()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolver);
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsProtectedApi_shouldResolveAccessDeniedException()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/api/v1/teachers");

    filter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
    verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
    verify(filterChain, never()).doFilter(any(), any());

    Exception resolved = captor.getValue();
    assertInstanceOf(AccessDeniedException.class, resolved);
    assertEquals("AUTH_FORCE_PASSWORD_CHANGE", resolved.getMessage());
  }

  @Test
  void doFilterInternal_whenForcePasswordChangeTrue_andPathIsProtectedUserApi_shouldResolveAccessDeniedException()
      throws Exception {
    User user =
        User.builder()
            .id(1L)
            .username("teacher1")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(true)
            .build();

    setAuthenticatedUser(user);
    when(request.getRequestURI()).thenReturn("/api/v1/users/1");

    filter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
    verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
    verify(filterChain, never()).doFilter(any(), any());

    Exception resolved = captor.getValue();
    assertInstanceOf(AccessDeniedException.class, resolved);
    assertEquals("AUTH_FORCE_PASSWORD_CHANGE", resolved.getMessage());
  }
}
