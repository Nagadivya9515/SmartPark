import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { catchError, map, of } from 'rxjs';

function isAdminRole(roles: string | undefined | null): boolean {
  if (!roles) return false;
  // Backend role is returned as "ROLE_ADMIN" etc
  return roles.includes('ROLE_ADMIN') || roles.includes('ADMIN');
}

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // If we have user info in memory and it's admin
  const u = auth.user();
  if (u && isAdminRole(u.roles)) return true;

  // Otherwise, try restore from cookie and verify role
  return auth.restoreSession().pipe(
    map(() => {
      const uu = auth.user();
      if (uu && isAdminRole(uu.roles)) return true;
      router.navigate(['/login']);
      return false;
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};

