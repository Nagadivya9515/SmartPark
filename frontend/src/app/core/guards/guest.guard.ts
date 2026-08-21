import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  if (authService.isLoggedIn()) {
    router.navigate(['/dashboard']);
    return false;
  }

  // Check server-side cookie — if session is valid, redirect away
  return authService.restoreSession().pipe(
    map(() => {
      router.navigate(['/dashboard']);
      return false;
    }),
    catchError(() => of(true)) // no session → allow access to login/register
  );
};
