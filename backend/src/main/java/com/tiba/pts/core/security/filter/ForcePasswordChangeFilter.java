package com.tiba.pts.core.security.filter;

import com.tiba.pts.modules.user.domain.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

/**
 * Filter that enforces password change policy.
 * When an authenticated user has forcePasswordChange == true, all API requests
 * are intercepted and rejected with 403 Forbidden, except for the password
 * change and logout endpoints.
 */
@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

  private final HandlerExceptionResolver resolver;

  private static final List<String> ALLOWED_EXACT_PATHS =
      List.of(
          "/api/v1/users/change-password",
          "/api/users/change-password",
          "/api/v1/users/change-password-first-login",
          "/api/users/change-password-first-login",
          "/api/auth/logout",
          "/api/v1/auth/logout",
          "/swagger-ui.html");

  private static final List<String> ALLOWED_PREFIXES =
      List.of(
          "/v3/api-docs",
          "/swagger-ui",
          "/h2-console");

  public ForcePasswordChangeFilter(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Always allow CORS pre-flight requests
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
      if (user.isForcePasswordChange()) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
          path = path.substring(contextPath.length());
        }

        if (!isPathAllowed(path)) {
          resolver.resolveException(
              request,
              response,
              null,
              new AccessDeniedException("AUTH_FORCE_PASSWORD_CHANGE"));
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean isPathAllowed(String path) {
    if (path == null) {
      return false;
    }

    String normalizedPath =
        path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;

    for (String allowed : ALLOWED_EXACT_PATHS) {
      if (normalizedPath.equalsIgnoreCase(allowed)) {
        return true;
      }
    }

    for (String prefix : ALLOWED_PREFIXES) {
      if (normalizedPath.toLowerCase().startsWith(prefix.toLowerCase())) {
        return true;
      }
    }

    return false;
  }
}
