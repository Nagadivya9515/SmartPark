import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  // If we already have user in memory → allow immediately
  if (authService.isLoggedIn()) {
    return true;
  }

  // Otherwise try to restore session from the HTTP-only cookie (page reload case)
  return authService.restoreSession().pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
