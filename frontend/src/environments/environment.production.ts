/** Build production (`ng build`). */
export const environment = {
  production: true,
  /** Si le même reverse-proxy sert le SPA et `/api`, laissez ''. Sinon : ex. 'https://api.mondomaine.com'. */
  apiBaseUrl: '',
  practitionerDoctorId: null as string | null,
};
