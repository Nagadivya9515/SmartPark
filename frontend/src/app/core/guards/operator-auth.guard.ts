import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OperatorService } from '../services/operator.service';

/** For operator-only routes (op_token in localStorage), not parking-user cookies. */
export const operatorAuthGuard: CanActivateFn = () => {
  const operator = inject(OperatorService);
  const router = inject(Router);

  if (operator.isLoggedIn) {
    return true;
  }
  router.navigate(['/operator/login']);
  return false;
};
