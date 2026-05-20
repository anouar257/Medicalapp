import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthProService } from '../services/auth-pro.service';

/**
 * Guard d'accès aux espaces professionnels — exige un JWT pro valide.
 */
export const authProGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authPro = inject(AuthProService);

  if (authPro.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login-pro']);
  return false;
};
