/**
 * Configuration par défaut (build **development** et `ng serve`).
 * Pour la prod, le build utilise `environment.production.ts` (voir `angular.json`).
 */
export const environment = {
  production: false,
  /**
   * Base de l’API. En dev : URL directe vers Spring Boot évite les réponses HTML (SPA) faussant le JSON.
   * Le backend expose CORS pour localhost. Alternative : chaîne vide + `ng serve` + proxy.conf.json.
   */
  apiBaseUrl: 'http://localhost:8081',
  /**
   * Identifiant du médecin pour le bouton « Mon agenda » (optionnel).
   * Sinon : `localStorage` clé `mediagenda-practitioner-doctor-id`.
   */
  practitionerDoctorId: null as string | null,
};
