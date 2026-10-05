package com.tiba.pts.core.security.config;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.security.filter.ForcePasswordChangeFilter;
import com.tiba.pts.core.security.jwt.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/login", "/api/auth/refresh", "/api/auth/logout"
  };

  private static final String[] API_DOCS_ENDPOINTS = {
    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  private final JwtAuthFilter jwtAuthFilter;
  private final ForcePasswordChangeFilter forcePasswordChangeFilter;
  private final Environment environment;
  private final ObjectMapper objectMapper;

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

              if (isDev) {
                auth.requestMatchers(API_DOCS_ENDPOINTS).permitAll();
                auth.requestMatchers("/h2-console/**").permitAll();
              }

              auth.anyRequest().authenticated();
            })
        .headers(
            headers -> {
              if (isDev) {
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
              } else {
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                headers.contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                headers.referrerPolicy(
                    rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
              }
            })
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(forcePasswordChangeFilter, JwtAuthFilter.class);

    return http.build();
  }

  @Bean
  public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration() {
    FilterRegistrationBean<JwtAuthFilter> registration =
        new FilterRegistrationBean<>(jwtAuthFilter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<ForcePasswordChangeFilter> forcePasswordChangeFilterRegistration() {
    FilterRegistrationBean<ForcePasswordChangeFilter> registration =
        new FilterRegistrationBean<>(forcePasswordChangeFilter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());

      ApiResponse<Void> body =
          ApiResponse.error(
              "Full authentication is required to access this resource", "AUTH_UNAUTHORIZED");
      objectMapper.writeValue(response.getOutputStream(), body);
    };
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();

    if (origins.isEmpty() || origins.contains("*")) {
      throw new IllegalStateException(
          "app.cors.allowed-origins must list explicit origins (wildcard is forbidden)");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
    configuration.setExposedHeaders(List.of("Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
