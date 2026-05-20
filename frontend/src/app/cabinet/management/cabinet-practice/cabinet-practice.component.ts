import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../../services/practitioner.service';
import { AuthProService } from '../../../services/auth-pro.service';
import { PreferencesService } from '../../../services/preferences.service';
import { MedicalOrganizationDTO } from '../../../models/practitioner.model';

type PracticeLoad = { kind: 'ok'; cabinet: MedicalOrganizationDTO } | { kind: 'no-org' } | { kind: 'error' };

/** Fiche et coordonnées du cabinet (praticien) — données issues de l’API practitioner. */
@Component({
  selector: 'app-cabinet-practice',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cabinet-practice.component.html',
  styleUrls: ['./cabinet-practice.component.scss'],
})
export class CabinetPracticeComponent {
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly authPro = inject(AuthProService);
  private readonly destroyRef = inject(DestroyRef);

  cabinet: MedicalOrganizationDTO | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';

  constructor() {
    toObservable(this.authPro.organizationId)
      .pipe(
        distinctUntilChanged(),
        switchMap((orgId): Observable<PracticeLoad> => {
          if (!orgId) {
            return of({ kind: 'no-org' as const });
          }
          this.loading = true;
          this.errorMessage = '';
          return this.practitionerService.getCabinet(orgId).pipe(
            map((cabinet) => ({ kind: 'ok' as const, cabinet })),
            catchError(() => of({ kind: 'error' as const })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        if (r.kind === 'no-org') {
          this.cabinet = null;
          this.loading = false;
          this.errorMessage = this.prefs.translate('cabinet.practice.noOrganization');
          return;
        }
        if (r.kind === 'error') {
          this.cabinet = null;
          this.loading = false;
          this.errorMessage = this.prefs.translate('cabinet.practice.loadError');
          return;
        }
        this.cabinet = r.cabinet;
        this.loading = false;
        this.errorMessage = '';
      });
  }

  save() {
    if (!this.cabinet) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.practitionerService.updateCabinet(this.cabinet.id, this.cabinet).subscribe({
      next: (c) => {
        this.cabinet = c;
        this.saving = false;
        this.successMessage = this.prefs.translate('cabinet.practice.saveSuccess');
        const u = this.authPro.getCurrentUser();
        if (u && c.nom) {
          this.authPro.patchCurrentUser({ organizationNom: c.nom });
        }
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.errorMessage =
          typeof e.error?.error === 'string' && e.error.error
            ? e.error.error
            : this.prefs.translate('cabinet.practice.saveError');
      },
    });
  }
}
