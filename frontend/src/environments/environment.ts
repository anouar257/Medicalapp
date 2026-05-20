/**
 * Configuration par défaut (build **development** et `ng serve`).
 * Pour la prod, le build utilise `environment.production.ts` (voir `angular.json`).
 */
export const environment = {
  production: false,
  /** Base de l'API Agenda (agenda-service — port 8081). */
  apiBaseUrl: 'http://localhost:8081',
  /** Base de l'API Patient (patient-service — port 8082). */
  patientApiBaseUrl: 'http://localhost:8082',
  /**
   * Base de l'API Praticien (practitioner-service — port 8083).
   * Comptes pro : praticien, assistant, administration plateforme ; catalogues et profils.
   */
  practitionerApiBaseUrl: 'http://localhost:8083',
  /**
   * Messagerie — en dev, chemin relatif servi par `proxy.conf.json` vers le port 8084
   * (évite CORS et les blocages navigateur vers un autre port).
   */
  messagingApiBaseUrl: '/messaging-api',
};
