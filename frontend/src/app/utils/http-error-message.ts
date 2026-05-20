/**
 * Extrait un message lisible depuis une erreur HttpClient / réponses Spring Boot
 * (Problem Details, {@code Map.of("error", ...)} , erreurs de validation {@code errors}, etc.).
 */
export function httpErrorBodyMessage(error: unknown): string | null {
  const e = error as { error?: unknown };
  const body = e?.error;
  if (body == null || body === '') {
    return null;
  }
  if (typeof body === 'string') {
    const t = body.trim();
    return t.length > 400 ? `${t.slice(0, 400)}…` : t;
  }
  if (typeof body !== 'object') {
    return null;
  }
  const o = body as Record<string, unknown>;
  if (typeof o['error'] === 'string' && o['error']) {
    return o['error'];
  }
  if (typeof o['detail'] === 'string' && o['detail']) {
    return o['detail'];
  }
  if (typeof o['message'] === 'string' && o['message']) {
    return o['message'];
  }
  const errs = o['errors'];
  if (errs && typeof errs === 'object' && !Array.isArray(errs)) {
    const parts: string[] = [];
    for (const [field, messages] of Object.entries(errs as Record<string, unknown>)) {
      if (Array.isArray(messages)) {
        for (const m of messages) {
          if (typeof m === 'string') {
            parts.push(`${field}: ${m}`);
          }
        }
      } else if (typeof messages === 'string') {
        parts.push(`${field}: ${messages}`);
      }
    }
    if (parts.length) {
      return parts.join(' · ');
    }
  }
  if (Array.isArray(errs)) {
    const parts = errs
      .map((x: unknown) => {
        if (typeof x === 'string') {
          return x;
        }
        if (x && typeof x === 'object') {
          const fe = x as Record<string, unknown>;
          const msg = fe['defaultMessage'] ?? fe['message'];
          const field = fe['field'] ?? fe['property'];
          if (typeof msg === 'string') {
            return typeof field === 'string' ? `${field}: ${msg}` : msg;
          }
        }
        return null;
      })
      .filter((s): s is string => !!s);
    if (parts.length) {
      return parts.join(' · ');
    }
  }
  return null;
}

/** Message utilisateur pour échec réseau / CORS / serveur arrêté. */
export function formatHttpError(error: unknown, fallback: string): string {
  const e = error as { status?: number; message?: string };
  if (e?.status === 0) {
    return 'Impossible de joindre le serveur (vérifiez qu\'il est démarré et le proxy / l\'URL).';
  }
  return httpErrorBodyMessage(error) ?? e?.message ?? fallback;
}
