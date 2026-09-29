import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { AuthService } from './auth.service';
import { AuthControllerService } from '../api/api/auth-controller.service';
import { ApiResponseAuthData, ApiResponseTokenRefreshResponse } from '../api/model/models';

describe('AuthService', () => {
  let service: AuthService;
  let mockAuthApi: Partial<AuthControllerService>;
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let mockHttp: Partial<HttpClient>;

  beforeEach(() => {
    localStorage.clear();

    mockAuthApi = {
      login: vi.fn(),
      refreshToken: vi.fn(),
      logout: vi.fn().mockReturnValue(of({ success: true, message: 'AUTH_LOGOUT_SUCCESS', data: null }))
    };

    mockRouter = {
      navigate: vi.fn()
    };

    mockHttp = {
      post: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: AuthControllerService, useValue: mockAuthApi },
        { provide: Router, useValue: mockRouter },
        { provide: HttpClient, useValue: mockHttp }
      ]
    });

    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('devrait stocker token et refreshToken lors d\'un login réussi', () => {
    const fakeAuthResponse: ApiResponseAuthData = {
      success: true,
      message: 'AUTH_LOGIN_SUCCESS',
      data: {
        token: 'fake-access-token',
        refreshToken: 'fake-refresh-token-uuid-1',
        type: 'Bearer',
        expiresIn: 1800,
        user: {
          id: 1,
          username: 'admin',
          role: 'ROLE_ADMIN',
          forcePasswordChange: false
        }
      }
    };

    mockAuthApi.login = vi.fn().mockReturnValue(of(fakeAuthResponse));

    service.login({ username: 'admin', password: 'password' }).subscribe();

    expect(localStorage.getItem('token')).toBe('fake-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('fake-refresh-token-uuid-1');
    expect(localStorage.getItem('role')).toBe('ROLE_ADMIN');
    expect(service.currentUser()?.username).toBe('admin');
  });

  it('devrait rafraîchir le token et mettre à jour accessToken et refreshToken (rotation)', () => {
    localStorage.setItem('token', 'old-access-token');
    localStorage.setItem('refreshToken', 'old-refresh-token-uuid');

    const fakeRefreshResponse: ApiResponseTokenRefreshResponse = {
      success: true,
      message: 'AUTH_TOKEN_REFRESH_SUCCESS',
      data: {
        accessToken: 'new-rotated-access-token',
        refreshToken: 'new-rotated-refresh-token-uuid',
        tokenType: 'Bearer',
        expiresIn: 1800
      }
    };

    mockAuthApi.refreshToken = vi.fn().mockReturnValue(of(fakeRefreshResponse));

    service.refreshToken().subscribe({
      next: (res) => {
        expect(res.data?.accessToken).toBe('new-rotated-access-token');
      }
    });

    expect(mockAuthApi.refreshToken).toHaveBeenCalledWith({
      refreshToken: 'old-refresh-token-uuid'
    });
    expect(localStorage.getItem('token')).toBe('new-rotated-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('new-rotated-refresh-token-uuid');
  });

  it('devrait échouer refreshToken si aucun refreshToken n\'est présent dans le storage', () => {
    let errorCaught: unknown = null;
    service.refreshToken().subscribe({
      error: (err) => {
        errorCaught = err;
      }
    });

    expect(errorCaught).toBeTruthy();
    expect(mockAuthApi.refreshToken).not.toHaveBeenCalled();
  });

  it('devrait révoquer le refreshToken via l\'API puis nettoyer le storage lors du logout', () => {
    localStorage.setItem('token', 'active-token');
    localStorage.setItem('refreshToken', 'active-refresh-token');
    localStorage.setItem('role', 'ROLE_ADMIN');

    service.logout();

    expect(mockAuthApi.logout).toHaveBeenCalledWith({
      refreshToken: 'active-refresh-token'
    });
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('devrait nettoyer le storage et rediriger même si le logout API échoue', () => {
    localStorage.setItem('token', 'active-token');
    localStorage.setItem('refreshToken', 'active-refresh-token');

    mockAuthApi.logout = vi.fn().mockReturnValue(throwError(() => new Error('Server error')));

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('devrait valider hasValidToken si refreshToken est présent même si l\'access token est expiré', () => {
    // Création d'un faux JWT expiré (exp dans le passé)
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const expiredPayload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 3600 }));
    const expiredToken = `${header}.${expiredPayload}.signature`;

    localStorage.setItem('token', expiredToken);
    localStorage.setItem('refreshToken', 'valid-refresh-token-uuid');

    expect(service.hasValidToken()).toBe(true);
  });

  it('devrait retourner false et purger les données si aucun token ni refreshToken n\'est présent', () => {
    expect(service.hasValidToken()).toBe(false);
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });
});
