import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../../services/practitioner.service';
import { AuthProService } from '../../../services/auth-pro.service';
import { PreferencesService } from '../../../services/preferences.service';
import { MedicalOrganizationDTO, ProUserDTO } from '../../../models/practitioner.model';
import { AgendaStateService } from '../../../services/agenda-state.service';
import { Appointment } from '../../../models/agenda.model';

type CabinetLoadResult =
  | { kind: 'ok'; cabinet: MedicalOrganizationDTO; users: ProUserDTO[] }
  | { kind: 'no-org' }
  | { kind: 'error' };

/** Vue d’ensemble du cabinet : effectifs et raccourcis vers la gestion (praticien). */
@Component({
  selector: 'app-cabinet-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cabinet-dashboard.component.html',
  styleUrls: ['./cabinet-dashboard.component.scss'],
})
export class CabinetDashboardComponent {
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly authPro = inject(AuthProService);
  private readonly agendaService = inject(AgendaStateService);
  private readonly destroyRef = inject(DestroyRef);

  cabinet: MedicalOrganizationDTO | null = null;
  users: ProUserDTO[] = [];
  errorMessage = '';
  loading = true;

  // Option B: Rendez-vous du jour
  todayAppointments: Appointment[] = [];
  appointmentsLoading = false;
  activeFilter: 'TO_TREAT' | 'ALL' = 'TO_TREAT';

  get countToTreat(): number {
    return this.todayAppointments.filter(
      (a) => a.status !== 'COMPLETED' && a.status !== 'NO_SHOW' && a.status !== 'CANCELLED'
    ).length;
  }

  get filteredAppointments(): Appointment[] {
    if (this.activeFilter === 'TO_TREAT') {
      return this.todayAppointments.filter(
        (a) => a.status !== 'COMPLETED' && a.status !== 'NO_SHOW' && a.status !== 'CANCELLED'
      );
    }
    return this.todayAppointments;
  }

  get currentPractitionerProfileId(): string | null {
    const profId = this.authPro.practitionerProfileId();
    return profId ? String(profId) : null;
  }

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
        this.loadTodayAppointments();
      });
  }

  countByRole(role: string): number {
    return this.users.filter((u) => u.role === role).length;
  }

  loadTodayAppointments(): void {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const end = new Date(start.getTime() + 24 * 60 * 60 * 1000);

    this.appointmentsLoading = true;
    this.agendaService.loadAppointments(start, end).subscribe({
      next: (appts) => {
        const profId = this.currentPractitionerProfileId;
        if (profId) {
          this.todayAppointments = appts.filter((a) => a.doctorId === profId);
        } else {
          this.todayAppointments = appts;
        }
        this.todayAppointments.sort((a, b) => a.startTime.getTime() - b.startTime.getTime());
        this.appointmentsLoading = false;
      },
      error: () => {
        this.appointmentsLoading = false;
      },
    });
  }

  markAsCompleted(id: string): void {
    this.agendaService.updateAppointmentStatus(id, 'COMPLETED').subscribe({
      next: () => {
        this.loadTodayAppointments();
      },
    });
  }

  markAsNoShow(id: string): void {
    this.agendaService.updateAppointmentStatus(id, 'NO_SHOW').subscribe({
      next: () => {
        this.loadTodayAppointments();
      },
    });
  }

  formatAptTime(d: Date | string): string {
    const dateObj = d instanceof Date ? d : new Date(d);
    return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(dateObj);
  }

  resolveAptHex(hex: string | undefined | null): string {
    const s = (hex ?? '').trim();
    if (!s) return '#3B82F6';
    return s.startsWith('#') ? s : `#${s}`;
  }
}
