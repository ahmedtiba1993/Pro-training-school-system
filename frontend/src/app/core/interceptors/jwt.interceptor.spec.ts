import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHandlerFn,
  HttpRequest,
  HttpResponse,
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { authInterceptor, IS_RETRIED_REQUEST } from './jwt.interceptor';
import { AuthService } from '../auth/auth.service';

describe('authInterceptor', () => {
  let mockAuthService: {
    getToken: ReturnType<typeof vi.fn>;
    getRefreshToken: ReturnType<typeof vi.fn>;
    refreshToken: ReturnType<typeof vi.fn>;
    forceLogout: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    mockAuthService = {
      getToken: vi.fn(),
      getRefreshToken: vi.fn(),
      refreshToken: vi.fn(),
      forceLogout: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService }
      ]
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('devrait ajouter le header Authorization sur une requête protégée si un token existe', () => {
    mockAuthService.getToken.mockReturnValue('my-access-token');

    const req = new HttpRequest<unknown>('GET', '/api/courses');
    let capturedReq: HttpRequest<unknown> | null = null;
    const next: HttpHandlerFn = (r) => {
      capturedReq = r;
      return of(new HttpResponse({ status: 200 }));
    };

    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next).subscribe();
    });

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.get('Authorization')).toBe('Bearer my-access-token');
  });

  it('ne devrait PAS ajouter de header Authorization sur /api/auth/login et /api/auth/refresh', () => {
    mockAuthService.getToken.mockReturnValue('my-access-token');

    const loginReq = new HttpRequest<unknown>('POST', '/api/auth/login', {});
    const refreshReq = new HttpRequest<unknown>('POST', '/api/auth/refresh', {});

    let capturedLoginReq: HttpRequest<unknown> | null = null;
    let capturedRefreshReq: HttpRequest<unknown> | null = null;

    TestBed.runInInjectionContext(() => {
      authInterceptor(loginReq, (r) => {
        capturedLoginReq = r;
        return of(new HttpResponse({ status: 200 }));
      }).subscribe();

      authInterceptor(refreshReq, (r) => {
        capturedRefreshReq = r;
        return of(new HttpResponse({ status: 200 }));
      }).subscribe();
    });

    expect(capturedLoginReq!.headers.has('Authorization')).toBe(false);
    expect(capturedRefreshReq!.headers.has('Authorization')).toBe(false);
  });

  it('devrait rafraîchir le token et rejouer la requête lors d\'une erreur 401', () => {
    mockAuthService.getToken.mockReturnValue('expired-access-token');
    mockAuthService.getRefreshToken.mockReturnValue('valid-refresh-token');
    mockAuthService.refreshToken.mockReturnValue(
      of({
        success: true,
        data: {
          accessToken: 'refreshed-new-access-token',
          refreshToken: 'new-rotated-refresh-token'
        }
      })
    );

    const req = new HttpRequest<unknown>('GET', '/api/students');
    let callCount = 0;
    const handledRequests: HttpRequest<unknown>[] = [];

    const next: HttpHandlerFn = (r) => {
      callCount++;
      handledRequests.push(r);
      if (callCount === 1) {
        // Premier appel: 401 Unauthorized
        return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));
      }
      // Rejeu après refresh: 200 OK
      return of(new HttpResponse({ status: 200 }));
    };

    let completed = false;
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next).subscribe({
        next: () => {
          completed = true;
        }
      });
    });

    expect(mockAuthService.refreshToken).toHaveBeenCalled();
    expect(callCount).toBe(2);
    expect(handledRequests[1].headers.get('Authorization')).toBe('Bearer refreshed-new-access-token');
    expect(handledRequests[1].context.get(IS_RETRIED_REQUEST)).toBe(true);
    expect(completed).toBe(true);
  });

  it('devrait déconnecter si une erreur 401 survient et qu\'aucun refresh token n\'est disponible', () => {
    mockAuthService.getToken.mockReturnValue('expired-access-token');
    mockAuthService.getRefreshToken.mockReturnValue(null);

    const req = new HttpRequest<unknown>('GET', '/api/students');
    const next: HttpHandlerFn = () =>
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));

    let errorThrown = false;
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next).subscribe({
        error: () => {
          errorThrown = true;
        }
      });
    });

    expect(mockAuthService.forceLogout).toHaveBeenCalled();
    expect(errorThrown).toBe(true);
  });

  it('devrait déconnecter si le endpoint /api/auth/refresh lui-même retourne 401', () => {
    const refreshReq = new HttpRequest<unknown>('POST', '/api/auth/refresh', {
      refreshToken: 'bad-token'
    });
    const next: HttpHandlerFn = () =>
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));

    let errorThrown = false;
    TestBed.runInInjectionContext(() => {
      authInterceptor(refreshReq, next).subscribe({
        error: () => {
          errorThrown = true;
        }
      });
    });

    expect(mockAuthService.forceLogout).toHaveBeenCalled();
    expect(mockAuthService.refreshToken).not.toHaveBeenCalled();
    expect(errorThrown).toBe(true);
  });
});
