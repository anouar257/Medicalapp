/** Build production (`ng build`). */
export const environment = {
  production: true,
  /** Si le même reverse-proxy sert le SPA et `/api`, laissez ''. Sinon : ex. 'https://api.mondomaine.com'. */
  apiBaseUrl: '',
  /** En production, le reverse-proxy route vers le patient-service. */
  patientApiBaseUrl: '',
  /** En production, le reverse-proxy route vers le practitioner-service. */
  practitionerApiBaseUrl: '',
  /**
   * À configurer sur le reverse-proxy (même origine que le SPA) : ex. préfixe `/messaging-api`
   * → messaging-service, ou URL absolue `https://api...` si appel cross-origin.
   */
  messagingApiBaseUrl: '/messaging-api',
};
