import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';

// --- IMPORTS FROM SWAGGER-GENERATED CODE ---
// (Adjust the path if necessary)
import { AuthControllerService } from '../api/api/auth-controller.service';
import { AuthRequest, AuthResponse } from '../api/model/models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // Inject the Swagger-generated service
  private authApi = inject(AuthControllerService);
  private router = inject(Router);

  // Signal to store the currently logged-in user
  currentUser = signal<any>(null);

  // Login method
  login(request: AuthRequest) {
    // Call the generated .login method
    return this.authApi.login(request).pipe(
      tap((response: AuthResponse) => {
        if (response.success && response.data) {
          // Save the token
          localStorage.setItem('token', response.data.token!);

          // Update the user signal
          this.currentUser.set(response.data.user);

          // Save the user role
          localStorage.setItem('role', response.data.user?.role || '');
        }
      }),
    );
  }

  // Logout method
  logout() {
    // Clear all stored data
    localStorage.clear();
    this.currentUser.set(null);

    // Redirect to login page
    this.router.navigate(['/auth/login']);
  }
}
