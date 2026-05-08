import { AGENDA_API_BASE_URL } from '../services/agenda-state.service';

const PLACEHOLDER = '/assets/doctors/placeholder.svg';

/**
 * URL affichable pour un portrait médecin : chemins `/assets/...` (Angular), URLs absolues, ou
 * fichiers servis par l’API (`/api/doctor-files/...` → préfixe `AGENDA_API_BASE_URL`).
 */
export function resolveDoctorPhotoUrl(url: string | undefined | null): string {
  const u = (url ?? '').trim();
  if (!u) {
    return PLACEHOLDER;
  }
  if (u.startsWith('http://') || u.startsWith('https://')) {
    return u;
  }
  if (u.startsWith('/api/doctor-files/')) {
    const base = AGENDA_API_BASE_URL.replace(/\/$/, '');
    return `${base}${u}`;
  }
  return u;
}
