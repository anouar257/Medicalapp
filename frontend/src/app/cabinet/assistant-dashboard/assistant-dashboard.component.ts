import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, take } from 'rxjs';
import { AuthProService } from '../../services/auth-pro.service';
import { AgendaService, AppointmentCabinetPendingDTO, AppointmentStatus } from '../../services/agenda.service';
import { AgendaStateService } from '../../services/agenda-state.service';
import { PreferencesService } from '../../services/preferences.service';

@Component({
  selector: 'app-assistant-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './assistant-dashboard.component.html',
})
export class AssistantDashboardComponent {
  readonly prefs = inject(PreferencesService);
  private readonly agenda = inject(AgendaService);
  private readonly agendaState = inject(AgendaStateService);
  private readonly authPro = inject(AuthProService);
  private readonly destroyRef = inject(DestroyRef);

  pending: AppointmentCabinetPendingDTO[] = [];
  loading = true;
  loadError = '';
  busyId: number | null = null;
  actionError = '';

  constructor() {
    toObservable(this.authPro.organizationId)
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    this.agenda.listCabinetPending().pipe(take(1)).subscribe({
      next: (rows) => {
        this.pending = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.loadError = this.prefs.translate('ASSISTANT.PENDING.LOAD_ERROR');
        this.pending = [];
        this.loading = false;
      },
    });
  }

  patientLabel(row: AppointmentCabinetPendingDTO): string {
    const n = `${row.patientPrenom ?? ''} ${row.patientNom ?? ''}`.trim();
    if (n) return n;
    if (row.patientId != null) return `#${row.patientId}`;
    return this.prefs.translate('ASSISTANT.PENDING.UNKNOWN_PATIENT');
  }

  reasonLabel(row: AppointmentCabinetPendingDTO): string {
    const code = row.visitReasonCode?.trim();
    if (!code) return this.prefs.translate('COMMON.EM_DASH');
    const key = `booking.reason.${code}`;
    const t = this.prefs.translate(key);
    return t === key ? code : t;
  }

  formatWhen(iso: string): string {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleString(this.prefs.language(), {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  }

  confirm(row: AppointmentCabinetPendingDTO): void {
    this.patch(row.id, 'CONFIRMED');
  }

  cancelOrMove(row: AppointmentCabinetPendingDTO): void {
    this.patch(row.id, 'CANCELLED');
  }

  private patch(id: number, status: AppointmentStatus): void {
    this.actionError = '';
    this.busyId = id;
    this.agenda.patchAppointmentStatus(id, status).subscribe({
      next: () => {
        this.pending = this.pending.filter((p) => p.id !== id);
        this.agendaState.refreshAppointments();
        this.busyId = null;
      },
      error: () => {
        this.actionError = this.prefs.translate('ASSISTANT.PENDING.ERROR');
        this.busyId = null;
      },
    });
  }
}
