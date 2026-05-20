import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthProService } from '../services/auth-pro.service';

/**
 * Bloque l’espace pro tant que l’email n’est pas vérifié par OTP (claim {@code isVerified} sur le JWT).
 */
export const proEmailVerifiedGuard: CanActivateFn = () => {
  const authPro = inject(AuthProService);
  const router = inject(Router);
  const u = authPro.getCurrentUser();
  if (!u) {
    void router.navigate(['/auth/login-pro']);
    return false;
  }
  if (u.emailVerifie) {
    return true;
  }
  void router.navigate(['/auth/verify-otp-pro'], {
    queryParams: { email: u.email, telephone: u.telephone },
  });
  return false;
};
