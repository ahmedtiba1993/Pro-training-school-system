package com.tiba.pts.core.security.service;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Point unique de verification du statut d'un compte.
 * Utilise par le filtre JWT (chaque requete) et par la rotation du refresh token.
 */
@Component
public class AccountStatusValidator {

  public void validate(UserDetails user) {
    if (!user.isAccountNonLocked()) {
      throw new LockedException("AUTH_ACCOUNT_SUSPENDED");
    }
    if (!user.isEnabled() || !user.isAccountNonExpired()) {
      throw new DisabledException("AUTH_ACCOUNT_DISABLED");
    }
  }
}
