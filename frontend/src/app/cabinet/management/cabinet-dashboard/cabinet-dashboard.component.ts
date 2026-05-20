import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../../services/practitioner.service';
import { AuthProService } from '../../../services/auth-pro.service';
import { PreferencesService } from '../../../services/preferences.service';
import { MedicalOrganizationDTO, ProUserDTO } from '../../../models/practitioner.model';

type CabinetLoadResult =
  | { kind: 'ok'; cabinet: MedicalOrganizationDTO; users: ProUserDTO[] }
  | { kind: 'no-org' }
  | { kind: 'error' };

/** Vue d’ensemble du cabinet : effectifs et raccourcis vers la gestion (praticien). */
@Component({
  selector: 'app-cabinet-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cabinet-dashboard.component.html',
  styleUrls: ['./cabinet-dashboard.component.scss'],
})
export class CabinetDashboardComponent {
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly authPro = inject(AuthProService);
  private readonly destroyRef = inject(DestroyRef);

  cabinet: MedicalOrganizationDTO | null = null;
  users: ProUserDTO[] = [];
  errorMessage = '';
  loading = true;

  constructor() {
    toObservable(this.authPro.organizationId)
      .pipe(
        distinctUntilChanged(),
        switchMap((orgId): Observable<CabinetLoadResult> => {
          if (!orgId) {
            return of({ kind: 'no-org' as const });
          }
          this.loading = true;
          this.errorMessage = '';
          return forkJoin({
            cabinet: this.practitionerService.getCabinet(orgId),
            users: this.practitionerService.listCabinetUsers(orgId),
          }).pipe(
            map(({ cabinet, users }) => ({
              kind: 'ok' as const,
              cabinet,
              users: users ?? [],
            })),
            catchError(() => of({ kind: 'error' as const })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        if (r.kind === 'no-org') {
          this.cabinet = null;
          this.users = [];
          this.loading = false;
          this.errorMessage = 'cabinet.management.noOrganization';
          return;
        }
        if (r.kind === 'error') {
          this.cabinet = null;
          this.users = [];
          this.loading = false;
          this.errorMessage = 'cabinet.management.loadCabinetError';
          return;
        }
        this.cabinet = r.cabinet;
        this.users = r.users;
        this.loading = false;
        this.errorMessage = '';
      });
  }

  countByRole(role: string): number {
    return this.users.filter((u) => u.role === role).length;
  }
}
