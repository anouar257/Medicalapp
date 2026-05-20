import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { AuthProService } from '../services/auth-pro.service';

/** Snapshot du contexte pro au moment de la navigation (pour guards / composants si besoin). */
export interface CabinetWorkspaceContext {
  organizationId: number | null;
  practitionerProfileId: number | null;
}

/**
 * Résolveur : expose le contexte cabinet courant dans `route.parent.data['workspace']`
 * (enfant de `cabinet`). Les composants peuvent aussi lire directement {@link AuthProService}.
 */
export const cabinetWorkspaceContextResolver: ResolveFn<CabinetWorkspaceContext> = () => {
  const auth = inject(AuthProService);
  return {
    organizationId: auth.organizationId(),
    practitionerProfileId: auth.practitionerProfileId(),
  };
};
