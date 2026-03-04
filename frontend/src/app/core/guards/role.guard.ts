import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const roleGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const userRole = localStorage.getItem('role');
  const expectedRoles = route.data['roles'] as Array<string>;

  // If user is authenticated AND possesses the required role -> Access granted
  if (userRole && expectedRoles.includes(userRole)) {
    return true;
  }

  // If the user is NOT authenticated -> Redirect to Login page
  if (!userRole) {
    router.navigate(['/auth/login']);
    return false;
  }

  // If authenticated but lacks permission (e.g., Teacher accessing Admin) -> Redirect to 403 Error
  router.navigate(['/error'], { queryParams: { code: '403' } });
  return false;
};
