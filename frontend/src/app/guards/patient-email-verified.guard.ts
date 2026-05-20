import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Bloque l’espace patient tant que l’email n’a pas été validé par OTP (cohérent avec le claim JWT
 * {@code isVerified} côté agenda / messagerie).
 */
export const patientEmailVerifiedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const p = auth.getCurrentPatient();
  if (!p) {
    void router.navigate(['/auth/login']);
    return false;
  }
  if (p.emailVerifie) {
    return true;
  }
  void router.navigate(['/auth/verify-otp'], {
    queryParams: { email: p.email, telephone: p.telephone },
  });
  return false;
};
