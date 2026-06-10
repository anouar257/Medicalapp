import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AuthProService } from '../services/auth-pro.service';

/**
 * Intercepteur HTTP unique pour les deux services d'authentification.
 *
 * <p>Strategie basee sur le chemin de l'URL:
 * <ul>
 *   <li>`/api/pro/public/...` et `/api/pro/specialties/public/...` -> aucun token</li>
 *   <li>`/api/pro/...` -> token pro</li>
 *   <li>`/api/auth...`, `/api/patients...`, `/api/proches...` -> token patient</li>
 *   <li>`/api/appointments/patient-booking`, `/api/appointments/patient/...`,
 *       `POST /api/reviews` -> token patient</li>
 *   <li>`/api/payments/...` -> token pro</li>
 *   <li>`/messaging-api/...` -> pro si disponible, sinon patient</li>
 *   <li>`/api/...` -> pro si disponible, sinon patient</li>
 * </ul>
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const patientAuth = inject(AuthService);
  const proAuth = inject(AuthProService);

  let pathname: string;
  try {
    pathname = new URL(req.url, 'http://localhost').pathname;
  } catch {
    pathname = req.url;
  }

  const isPublicPro =
    pathname.includes('/api/pro/public/') ||
    pathname.includes('/api/pro/specialties/public/');
  if (isPublicPro) {
    return next(req);
  }

  const isProPath = pathname.startsWith('/api/pro/');
  const isPaymentPath = pathname.startsWith('/api/payments/');
  const isPatientPath =
    pathname.startsWith('/api/auth') ||
    pathname.startsWith('/api/patients') ||
    pathname.startsWith('/api/proches');
  const isPatientAgendaPath =
    pathname === '/api/appointments/patient-booking' ||
    pathname.startsWith('/api/appointments/patient/') ||
    (pathname === '/api/reviews' && req.method === 'POST');
  const isMessagingPath = pathname.startsWith('/messaging-api');

  let token: string | null = null;

  if (isProPath || isPaymentPath) {
    token = proAuth.getToken();
  } else if (isPatientPath || isPatientAgendaPath) {
    token = patientAuth.getToken();
  } else if (isMessagingPath) {
    token = proAuth.getToken() ?? patientAuth.getToken();
  } else {
    token = proAuth.getToken() ?? patientAuth.getToken();
  }

  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
