import { AGENDA_API_BASE_URL } from '../services/agenda-state.service';

const PLACEHOLDER = '/assets/doctors/placeholder.svg';

/**
 * URL affichable pour un portrait médecin : chemins `/assets/...` (Angular), URLs absolues, ou
 * fichiers servis par l'API (`/api/doctor-files/...` → préfixe `AGENDA_API_BASE_URL`).
 *
 * Gère aussi les URLs stockées en base avec un host local (ex. `http://localhost:8081/api/doctor-files/...`)
 * en les normalisant vers le chemin relatif pour que le reverse-proxy (Nginx) puisse les servir.
 */

export function getDynamicAvatar(name: string): string {
  const cleanName = name.replace(/Dr\.?\s*/ig, '').replace(/Pr\.?\s*/ig, '').trim() || 'MD';
  const parts = cleanName.split(' ').filter(p => p.length > 0);
  let initials = '';
  if (parts.length >= 2) {
    initials = (parts[0][0] + parts[1][0]).toUpperCase();
  } else if (parts.length === 1) {
    initials = parts[0].substring(0, 2).toUpperCase();
  } else {
    initials = 'MD';
  }

  // Generate a consistent professional color based on the name
  const colors = ['#2563eb', '#0ea5e9', '#0d9488', '#4f46e5', '#7c3aed', '#db2777', '#ea580c'];
  let hash = 0;
  for (let i = 0; i < cleanName.length; i++) {
    hash = cleanName.charCodeAt(i) + ((hash << 5) - hash);
  }
  const color = colors[Math.abs(hash) % colors.length];

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
    <rect width="100" height="100" fill="${color}" />
    <text x="50" y="50" font-family="Inter, system-ui, sans-serif" font-weight="700" font-size="40" fill="#ffffff" text-anchor="middle" dy="0.35em">${initials}</text>
  </svg>`;

  return `data:image/svg+xml;base64,${btoa(svg)}`;
}

export function resolveDoctorPhotoUrl(url: string | undefined | null, name?: string): string {
  let u = (url ?? '').trim();
  if (!u) {
    return name ? getDynamicAvatar(name) : PLACEHOLDER;
  }

  // Normaliser les URLs absolues vers localhost:808x qui pointent vers /api/
  // → les convertir en chemin relatif pour passer par le reverse-proxy
  const doctorFilesMatch = u.match(/^https?:\/\/localhost:\d+(\/api\/.*)/);
  if (doctorFilesMatch) {
    u = doctorFilesMatch[1];
  }

  if (u.startsWith('http://') || u.startsWith('https://')) {
    return u;
  }
  if (u.startsWith('/api/')) {
    const base = AGENDA_API_BASE_URL.replace(/\/$/, '');
    return `${base}${u}`;
  }
  return u;
}
