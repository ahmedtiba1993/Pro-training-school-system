import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService); // Injecting your AuthService

  // Check if the token is valid and not expired.
  // (The hasValidToken method will automatically remove the token/role from localStorage if expired)
  if (!authService.hasValidToken()) {
    router.navigate(['/auth/login']);
    return false;
  }

  // If the token is valid, verify the roles
  const userRole = localStorage.getItem('role');
  const expectedRoles = route.data['roles'] as Array<string>;

  // If the user possesses the required role -> Access granted
  if (userRole && expectedRoles && expectedRoles.includes(userRole)) {
    return true;
  }

  // If there is no role in localStorage (anomaly) -> Redirect to Login
  if (!userRole) {
    router.navigate(['/auth/login']);
    return false;
  }

  // If authenticated but lacks permission (e.g., Teacher trying to access Admin) -> 403 Error
  router.navigate(['/error'], { queryParams: { code: '403' } });
  return false;
};
