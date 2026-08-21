import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Prevents multiple simultaneous refresh calls
let isRefreshing = false;
const refreshDone$ = new BehaviorSubject<boolean>(false);

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  // Always send cookies with every request (access_token / refresh_token)
  const reqWithCredentials = req.clone({ withCredentials: true });

  return next(reqWithCredentials).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint =
        req.url.includes('/api/auth/login') ||
        req.url.includes('/api/auth/register') ||
        req.url.includes('/api/auth/refresh') ||
        req.url.includes('/api/auth/logout') ||
        req.url.includes('/api/auth/logout-all') ||
        req.url.includes('/api/auth/me');

      // Only attempt refresh for user-authenticated endpoints (cookie-based),
      // never for operator endpoints (Bearer token) to avoid incorrect refresh loops.
      const isOperatorEndpoint = req.url.includes('/api/operator');

      if (error.status === 401 && !isAuthEndpoint && !isOperatorEndpoint) {
        return handleUnauthorized(reqWithCredentials, next, authService);
      }

      return throwError(() => error);
    })
  );
};

function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
) {
  if (!isRefreshing) {
    // ── Start refresh ──────────────────────────────────────────────────
    isRefreshing = true;
    refreshDone$.next(false);

    return authService.refreshToken().pipe(
      switchMap(() => {
        isRefreshing = false;
        refreshDone$.next(true);
        // Retry the original request with the new cookie
        return next(req.clone({ withCredentials: true }));
      }),
      catchError(err => {
        isRefreshing = false;
        refreshDone$.next(false);
        // Refresh failed → force logout (clears cookie server-side + navigates)
        authService.logout();
        return throwError(() => err);
      })
    );
  } else {
    // ── Another request is already refreshing — queue it ──────────────
    return refreshDone$.pipe(
      filter(done => done === true),
      take(1),
      switchMap(() => next(req.clone({ withCredentials: true })))
    );
  }
}
