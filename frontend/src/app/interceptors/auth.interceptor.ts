import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AuthProService } from '../services/auth-pro.service';
import { environment } from '../../environments/environment';

/**
 * Intercepteur HTTP unique pour les deux services d'authentification.
 *
 * <p>Stratégie :
 * <ul>
 *   <li>requête vers le practitioner-service ({@code /api/pro/...}) → utilise le token pro ;</li>
 *   <li>requête vers le patient-service ({@code /api/auth} ou {@code /api/...} sur le port patient)
 *       → utilise le token patient ;</li>
 *   <li>requête vers l'agenda-service → injecte le token pro si présent (l'agenda est partagé
 *       entre les rôles du cabinet), sinon le token patient si présent (lecture côté patient).</li>
 * </ul>
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const patientAuth = inject(AuthService);
  const proAuth = inject(AuthProService);

  const url = req.url;
  const isPro = url.includes('/api/pro/') || url.startsWith(environment.practitionerApiBaseUrl);
  const isPatient = url.startsWith(environment.patientApiBaseUrl) && !url.includes('/api/pro/');
  const messagingConfigured = (environment.messagingApiBaseUrl ?? '').trim();
  const messagingBase = messagingConfigured.replace(/\/$/, '');
  let isMessaging = false;
  if (messagingBase.length > 0) {
    if (messagingBase.startsWith('http://') || messagingBase.startsWith('https://')) {
      isMessaging = url.startsWith(messagingBase);
    } else {
      const pathPrefix = messagingBase.startsWith('/') ? messagingBase : `/${messagingBase}`;
      try {
        const pathname = new URL(url).pathname;
        isMessaging = pathname === pathPrefix || pathname.startsWith(`${pathPrefix}/`);
      } catch {
        isMessaging = url.includes(`${pathPrefix}/`) || url.endsWith(pathPrefix);
      }
    }
  }

  let token: string | null = null;
  if (isPro) {
    token = proAuth.getToken();
  } else if (isPatient) {
    token = patientAuth.getToken();
  } else if (isMessaging) {
    token = proAuth.getToken() ?? patientAuth.getToken();
  } else {
    token = proAuth.getToken() ?? patientAuth.getToken();
  }

  if (token) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next(cloned);
  }
  return next(req);
};
