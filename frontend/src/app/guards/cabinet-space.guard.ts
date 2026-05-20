import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthProService } from '../services/auth-pro.service';

/**
 * L’espace /cabinet est réservé aux comptes rattachés à un cabinet ; l’administration plateforme est redirigée vers /platform-admin.
 */
export const cabinetSpaceGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authPro = inject(AuthProService);

  if (!authPro.isAuthenticated()) {
    router.navigate(['/auth/login-pro']);
    return false;
  }

  if (authPro.getRole() === 'ADMIN' && authPro.getCurrentUser()?.organizationId == null) {
    router.navigate(['/platform-admin'], { replaceUrl: true });
    return false;
  }

  return true;
};
