import { AsyncPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import type { Appointment } from '../../models/agenda.model';
import type { AgendaView } from '../../models/agenda.model';
import { AppointmentModalComponent } from '../appointment-modal/appointment-modal.component';
import { CalendarGridComponent } from '../calendar-grid/calendar-grid.component';
import { SidebarFilterComponent } from '../sidebar-filter/sidebar-filter.component';
import { AgendaStateService } from '../../services/agenda-state.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-agenda-dashboard',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterLink,
    SidebarFilterComponent,
    CalendarGridComponent,
    AppointmentModalComponent,
  ],
  templateUrl: './agenda-dashboard.component.html',
  styleUrls: ['./agenda-dashboard.component.scss'],
})
export class AgendaDashboardComponent {
  private readonly agenda = inject(AgendaStateService);

  readonly theme = inject(ThemeService);

  /** Identifiant médecin agenda : `localStorage` clé `medconnect-practitioner-doctor-id` (optionnel). */
  readonly practitionerDoctorId: string | null = (() => {
    try {
      return typeof localStorage !== 'undefined'
        ? localStorage.getItem('medconnect-practitioner-doctor-id')?.trim() || null
        : null;
    } catch {
      return null;
    }
  })();

  readonly modalOpen = signal(false);

  /** Rendez-vous passé au modal en mode édition (`null` pour une création). */
  readonly modalEditAppointment = signal<Appointment | null>(null);

  readonly currentView$ = this.agenda.currentView$;

  readonly mobileSidebarOpen = toSignal(this.agenda.mobileSidebarOpen$, {
    initialValue: false,
  });

  readonly viewItems: readonly { view: AgendaView; label: string }[] = [
    { view: 'day', label: 'Jour' },
    { view: 'week', label: 'Semaine' },
    { view: 'month', label: 'Mois' },
    { view: 'year', label: 'Année' },
  ];

  filterMyAgenda(): void {
    const id = this.practitionerDoctorId;
    if (!id) {
      return;
    }
    this.agenda.setSelectedDoctorIds([id]);
  }

  setView(view: AgendaView): void {
    this.agenda.setCurrentView(view);
  }

  /** Ramène la date sélectionnée sur le jour courant (midi local, même repère que le reste du calendrier). */
  goToToday(): void {
    const t = new Date();
    t.setHours(12, 0, 0, 0);
    this.agenda.setSelectedDate(t);
  }

  toggleMobileSidebar(): void {
    this.agenda.toggleMobileSidebar();
  }

  closeMobileSidebar(): void {
    this.agenda.closeMobileSidebar();
  }

  openNewAppointment(): void {
    this.modalEditAppointment.set(null);
    this.modalOpen.set(true);
  }

  openAppointmentFromGrid(apt: Appointment): void {
    this.modalEditAppointment.set({
      ...apt,
      startTime: new Date(apt.startTime),
      endTime: new Date(apt.endTime),
    });
    this.modalOpen.set(true);
  }

  onModalClosed(): void {
    this.modalOpen.set(false);
    this.modalEditAppointment.set(null);
  }
}
