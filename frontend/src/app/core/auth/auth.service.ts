import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, tap, timeout } from 'rxjs/operators';
import { BASE_PATH } from '../api/variables';

// --- IMPORTS FROM SWAGGER-GENERATED CODE ---
import { AuthControllerService } from '../api/api/auth-controller.service';
import {
  ApiResponseAuthData,
  ApiResponseTokenRefreshResponse,
  AuthRequest,
  UserInfo
} from '../api/model/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
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

  // Token helper methods
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  setRefreshToken(refreshToken: string): void {
    localStorage.setItem('refreshToken', refreshToken);
  }

  clearAuthData(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
    localStorage.removeItem('forcePasswordChange');
    this.currentUser.set(null);
  }

  // Login method
  login(request: AuthRequest): Observable<ApiResponseAuthData> {
    return this.authApi.login(request).pipe(
      tap((response: ApiResponseAuthData) => {
        if (response.success && response.data) {
          // Save the access token
          if (response.data.token) {
            this.setToken(response.data.token);
          }

          // Save the refresh token
          if (response.data.refreshToken) {
            this.setRefreshToken(response.data.refreshToken);
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

  // Refresh token method
  refreshToken(): Observable<ApiResponseTokenRefreshResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('NO_REFRESH_TOKEN'));
    }

    return this.authApi.refreshToken({ refreshToken }).pipe(
      tap((response: ApiResponseTokenRefreshResponse) => {
        if (response.success && response.data) {
          // Update access token
          if (response.data.accessToken) {
            this.setToken(response.data.accessToken);
          }
          // Update rotated refresh token
          if (response.data.refreshToken) {
            this.setRefreshToken(response.data.refreshToken);
          }
        }
      })
    );
  }

  // Logout method (calls backend to revoke refresh token, then clears local storage)
  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.authApi.logout({ refreshToken }).pipe(
        timeout(3000),
        catchError(() => of(null)),
        finalize(() => {
          this.forceLogout();
        })
      ).subscribe();
    } else {
      this.forceLogout();
    }
  }

  // Force local logout without backend call (used when tokens are expired/invalid)
  forceLogout(): void {
    this.clearAuthData();
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

  isAccessTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      const payloadBase64 = token.split('.')[1];
      const payloadDecoded = JSON.parse(atob(payloadBase64));
      const expirationDate = payloadDecoded.exp * 1000;
      return Date.now() >= expirationDate;
    } catch {
      return true;
    }
  }

  hasValidToken(): boolean {
    // If access token is valid and not expired
    if (!this.isAccessTokenExpired()) {
      return true;
    }

    // If access token is expired, but a refresh token is available,
    // consider session active because interceptor will seamlessly refresh it
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      return true;
    }

    // Neither access token nor refresh token is valid
    this.clearAuthData();
    return false;
  }
}
