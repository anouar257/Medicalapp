import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/patient.model';
import { AgendaService, AppointmentPatientDTO, AppointmentStatus } from '../../services/agenda.service';
import { PreferencesService } from '../../services/preferences.service';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, AppPreferencesToolbarComponent],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.scss'],
})
export class PatientDashboardComponent implements OnInit {
  patient: AuthResponse | null = null;

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly agendaService = inject(AgendaService);
  readonly prefs = inject(PreferencesService);

  appointments: AppointmentPatientDTO[] = [];
  appointmentsLoading = false;
  appointmentsError = '';

  readonly appointmentSkeletons = [1, 2, 3, 4] as const;

  ngOnInit() {
    this.patient = this.authService.getCurrentPatient();
    if (!this.patient) this.router.navigate(['/auth/login']);
    else this.loadAppointments();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  private loadAppointments(): void {
    if (!this.patient) return;
    this.appointmentsLoading = true;
    this.appointmentsError = '';

    this.agendaService.getAppointmentsForPatient(this.patient.patientId).subscribe({
      next: (list) => {
        this.appointments = list;
        this.appointmentsLoading = false;
      },
      error: (e) => {
        this.appointmentsLoading = false;
        this.appointmentsError = e?.error?.error || this.prefs.translate('Impossible de charger vos rendez-vous.');
      },
    });
  }

  statusLabel(status: AppointmentStatus | undefined): string {
    if (status === 'CONFIRMED') return this.prefs.translate('Confirmé');
    if (status === 'PENDING') return this.prefs.translate('En attente');
    if (status === 'CANCELLED') return this.prefs.translate('Annulé');
    return this.prefs.translate('Statut');
  }

  statusBadgeClasses(status: AppointmentStatus | undefined): string {
    if (status === 'CONFIRMED') return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300 border-emerald-200/80 dark:border-emerald-800/50';
    if (status === 'PENDING') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 border-amber-200/80 dark:border-amber-800/50';
    if (status === 'CANCELLED') return 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300 border-rose-200/80 dark:border-rose-800/50';
    return 'bg-slate-100 text-slate-700 dark:bg-slate-700/30 dark:text-slate-200 border-slate-200/80 dark:border-slate-600/50';
  }
}
