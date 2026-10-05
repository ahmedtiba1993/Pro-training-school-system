package com.tiba.pts.core.security.jwt;

import com.tiba.pts.core.security.service.AccountStatusValidator;
import com.tiba.pts.core.security.service.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Authentifie la requete a partir du Bearer token.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserDetailsServiceImpl userDetailsService;
  private final AccountStatusValidator accountStatusValidator;
  private final HandlerExceptionResolver resolver;

  public JwtAuthFilter(
      JwtService jwtService,
      UserDetailsServiceImpl userDetailsService,
      AccountStatusValidator accountStatusValidator,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.accountStatusValidator = accountStatusValidator;
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    // RFC 6750 : le schema "Bearer" est insensible a la casse
    if (authHeader == null
        || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      filterChain.doFilter(request, response);
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        authenticate(request, authHeader.substring(BEARER_PREFIX.length()).trim());
      } catch (JwtException | AuthenticationException ex) {
        SecurityContextHolder.clearContext();
        resolver.resolveException(request, response, null, ex);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private void authenticate(HttpServletRequest request, String jwt) {
    Claims claims = jwtService.parseAndValidate(jwt);
    String username = claims.getSubject();
    if (username == null || username.isBlank()) {
      throw new MalformedJwtException("AUTH_TOKEN_SUBJECT_MISSING");
    }

    UserDetails user = loadUser(username);
    accountStatusValidator.validate(user);

    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  private UserDetails loadUser(String username) {
    try {
      return userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException ex) {
      throw new JwtException("AUTH_TOKEN_USER_NOT_FOUND");
    }
  }
}
