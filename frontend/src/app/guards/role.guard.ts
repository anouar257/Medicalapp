import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthProService } from '../services/auth-pro.service';
import { ProUserRole } from '../models/practitioner.model';

/**
 * Construit un guard qui n'autorise l'accès qu'aux rôles donnés.
 *
 * <pre>
 *   { path: 'cabinet/dashboard', canActivate: [authProGuard, roleGuard(['PRATICIEN'])] }
 * </pre>
 */
export const roleGuard = (allowedRoles: ProUserRole[]): CanActivateFn => () => {
  const router = inject(Router);
  const authPro = inject(AuthProService);
  const role = authPro.getRole();

  if (!role) {
    router.navigate(['/auth/login-pro']);
    return false;
  }

  if (allowedRoles.includes(role)) {
    return true;
  }

  router.navigate([authPro.homeRouteForRole(role)]);
  return false;
};
