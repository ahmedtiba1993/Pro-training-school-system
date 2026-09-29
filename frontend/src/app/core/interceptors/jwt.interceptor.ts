import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn
} from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

/** Context token to flag retried requests and avoid infinite refresh loops */
export const IS_RETRIED_REQUEST = new HttpContextToken<boolean>(() => false);

// Refresh lock and queue for handling concurrent 401 responses
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isPublicAuthEndpoint =
    req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh');
  const isAuthEndpoint = isPublicAuthEndpoint || req.url.includes('/api/auth/logout');

  // Do not add Authorization header to public auth endpoints
  let authReq = req;
  const token = authService.getToken();

  if (!isPublicAuthEndpoint && token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If error is not 401, propagate directly
      if (error.status !== 401) {
        return throwError(() => error);
      }

      // If error occurs on auth endpoints, do NOT attempt to refresh
      if (isAuthEndpoint) {
        if (req.url.includes('/api/auth/refresh')) {
          // If refresh itself failed with 401, session is definitively expired
          isRefreshing = false;
          refreshTokenSubject.next(null);
          authService.forceLogout();
        }
        return throwError(() => error);
      }

      // If request has already been retried once and failed again, terminate session
      if (req.context.get(IS_RETRIED_REQUEST)) {
        authService.forceLogout();
        return throwError(() => error);
      }

      // If no refresh token is stored, logout immediately
      const refreshToken = authService.getRefreshToken();
      if (!refreshToken) {
        authService.forceLogout();
        return throwError(() => error);
      }

      // Handle token refresh concurrency
      return handle401Error(req, next, authService);
    })
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap(response => {
        isRefreshing = false;
        const newAccessToken = response.data?.accessToken;

        if (!newAccessToken) {
          authService.forceLogout();
          return throwError(() => new Error('EMPTY_REFRESH_TOKEN_RESPONSE'));
        }

        refreshTokenSubject.next(newAccessToken);

        // Replay original request with new access token
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${newAccessToken}`
            },
            context: req.context.set(IS_RETRIED_REQUEST, true)
          })
        );
      }),
      catchError(refreshError => {
        isRefreshing = false;
        refreshTokenSubject.next(null);
        authService.forceLogout();
        return throwError(() => refreshError);
      })
    );
  }

  // If a refresh is already in progress, wait until the new token is emitted
  return refreshTokenSubject.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap(newToken => {
      return next(
        req.clone({
          setHeaders: {
            Authorization: `Bearer ${newToken}`
          },
          context: req.context.set(IS_RETRIED_REQUEST, true)
        })
      );
    })
  );
}
