import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { BASE_PATH } from '../api/variables';

// --- IMPORTS FROM SWAGGER-GENERATED CODE ---
// (Adjust the path if necessary)
import { AuthControllerService } from '../api/api/auth-controller.service';
import { ApiResponseAuthData, AuthRequest, UserInfo } from '../api/model/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Inject the Swagger-generated service
  private authApi = inject(AuthControllerService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private basePath = inject(BASE_PATH, { optional: true }) || 'http://localhost:8080';

  // Signal to store the currently logged-in user
  currentUser = signal<UserInfo | null>(null);

  constructor() {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        this.currentUser.set(JSON.parse(storedUser));
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
  }

  // Login method
  login(request: AuthRequest): Observable<ApiResponseAuthData> {
    // Call the generated .login method
    return this.authApi.login(request).pipe(
      tap((response: ApiResponseAuthData) => {
        if (response.success && response.data) {
          // Save the token
          if (response.data.token) {
            localStorage.setItem('token', response.data.token);
          }

          // Update the user signal
          this.currentUser.set(response.data.user ?? null);

          // Save the user role
          localStorage.setItem('role', response.data.user?.role || '');

          // Save user object and forcePasswordChange flag
          if (response.data.user) {
            localStorage.setItem('user', JSON.stringify(response.data.user));
          }
          if (response.data.user?.forcePasswordChange) {
            localStorage.setItem('forcePasswordChange', 'true');
          } else {
            localStorage.removeItem('forcePasswordChange');
          }
        }
      })
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

  // Custom password change endpoint on first login
  changePasswordFirstLogin(newPassword: string, confirmNewPassword: string) {
    const baseUrl = Array.isArray(this.basePath) ? this.basePath[0] : this.basePath;
    const url = `${baseUrl}/api/v1/users/change-password-first-login`;
    return this.http.post<any>(url, {
      newPassword,
      confirmNewPassword
    });
  }

  hasValidToken(): boolean {
    const token = localStorage.getItem('token');

    if (!token) return false;

    try {
      const payloadBase64 = token.split('.')[1];
      const payloadDecoded = JSON.parse(atob(payloadBase64));
      const expirationDate = payloadDecoded.exp * 1000;

      const isValid = Date.now() < expirationDate;

      if (!isValid) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('user');
        localStorage.removeItem('forcePasswordChange');
        this.currentUser.set(null);
      }

      return isValid;
    } catch (e) {
      // Token malformé
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('user');
      localStorage.removeItem('forcePasswordChange');
      return false;
    }
  }
}
