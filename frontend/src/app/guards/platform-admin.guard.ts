import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthProService } from '../services/auth-pro.service';

/** Administration plateforme : compte ADMIN sans cabinet (JWT). */
export const platformAdminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authPro = inject(AuthProService);

  if (!authPro.isAuthenticated()) {
    router.navigate(['/auth/login-pro']);
    return false;
  }

  const u = authPro.getCurrentUser();
  if (u?.role === 'ADMIN' && u.organizationId == null) {
    return true;
  }

  router.navigate([authPro.homeRouteForRole(authPro.getRole())]);
  return false;
};
