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
import { environment } from '../../../environments/environment';

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
  template: `
    <div class="flex min-h-screen flex-col bg-slate-50 dark:bg-[#0F172A]">
      <header
        class="sticky top-0 z-40 flex flex-wrap items-center gap-3 border-b border-slate-200/80 bg-white px-3 py-3 shadow-sm dark:border-slate-700/80 dark:bg-[#1E293B] sm:gap-4 sm:px-4 md:px-8"
      >
        <button
          type="button"
          class="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-700 transition hover:bg-slate-100 md:hidden dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700/80"
          aria-label="Ouvrir le menu filtres"
          [attr.aria-expanded]="mobileSidebarOpen() ? 'true' : 'false'"
          aria-controls="agenda-sidebar-filters"
          (click)="toggleMobileSidebar()"
        >
          <span class="sr-only">Menu filtres</span>
          <svg class="size-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <a
          routerLink="/"
          class="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-700 transition hover:bg-slate-100 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700/80"
          aria-label="Planning"
        >
          <svg class="size-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" aria-hidden="true">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
            />
          </svg>
        </a>

        <div class="min-w-0 flex-1">
          <p class="text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-400 dark:text-slate-500">
            Planning
          </p>
          <h1 class="truncate text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100 md:text-xl">
            Agenda
          </h1>
        </div>

        <button
          type="button"
          class="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-700 transition hover:bg-slate-100 dark:border-slate-600 dark:bg-slate-800 dark:text-amber-300 dark:hover:bg-slate-700/80"
          [attr.aria-pressed]="theme.isDark()"
          aria-label="Basculer mode clair ou sombre"
          (click)="theme.toggle()"
        >
          @if (theme.isDark()) {
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
            </svg>
          } @else {
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          }
        </button>

        @if (currentView$ | async; as currentView) {
          <nav
            class="order-last flex w-full basis-full flex-wrap items-center justify-center gap-2 sm:order-none sm:w-auto sm:basis-auto md:flex-[2]"
            aria-label="Choix de la vue calendrier"
          >
            <button
              type="button"
              class="shrink-0 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700 sm:px-4 sm:text-sm"
              (click)="goToToday()"
            >
              Aujourd’hui
            </button>
            <div
              class="inline-flex max-w-full flex-wrap items-center justify-center gap-0.5 rounded-full bg-slate-100 p-1 ring-1 ring-slate-200/90 dark:bg-slate-800/90 dark:ring-slate-600/80"
            >
              @for (item of viewItems; track item.view) {
                <button
                  type="button"
                  class="inline-flex items-center justify-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-semibold transition hover:bg-white hover:text-slate-900 dark:hover:bg-slate-700 md:px-5 md:text-sm"
                  [class.bg-white]="currentView === item.view"
                  [class.text-slate-900]="currentView === item.view"
                  [class.shadow-sm]="currentView === item.view"
                  [class.text-slate-600]="currentView !== item.view"
                  [class.dark:bg-slate-600]="currentView === item.view"
                  [class.dark:text-slate-100]="currentView === item.view"
                  [class.dark:text-slate-400]="currentView !== item.view"
                  (click)="setView(item.view)"
                >
                  {{ item.label }}
                </button>
              }
            </div>
          </nav>
        }

        <div class="flex shrink-0 items-center gap-2">
          @if (practitionerDoctorId) {
            <button
              type="button"
              class="rounded-full border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700 sm:px-5"
              title="Afficher uniquement les créneaux de votre agenda"
              (click)="filterMyAgenda()"
            >
              Mon agenda
            </button>
          }
          <button
            type="button"
            class="rounded-full bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm shadow-blue-600/25 transition hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-offset-2 focus:ring-offset-white dark:focus:ring-offset-[#1E293B] sm:px-5"
            (click)="openNewAppointment()"
          >
            <span class="hidden sm:inline">+ Nouveau rendez-vous</span>
            <span class="sm:hidden" aria-hidden="true">+ RDV</span>
          </button>
        </div>
      </header>

      <div class="relative flex min-h-0 flex-1">
        @if (mobileSidebarOpen()) {
          <button
            type="button"
            class="fixed inset-0 z-40 bg-slate-950/45 backdrop-blur-[1px] md:hidden dark:bg-black/55"
            aria-label="Fermer le menu filtres"
            (click)="closeMobileSidebar()"
          ></button>
        }
        <app-sidebar-filter id="agenda-sidebar-filters" />
        <div class="flex min-w-0 flex-1 flex-col overflow-hidden bg-slate-50 dark:bg-[#0F172A]">
          <main class="scrollbar-calendar min-h-0 flex-1 overflow-y-auto p-4 md:p-8">
            <app-calendar-grid (appointmentOpened)="openAppointmentFromGrid($event)" />
          </main>
        </div>
      </div>

      <app-appointment-modal
        [open]="modalOpen()"
        [appointmentToEdit]="modalEditAppointment()"
        (closed)="onModalClosed()"
      />
    </div>
  `,
})
export class AgendaDashboardComponent {
  private readonly agenda = inject(AgendaStateService);

  readonly theme = inject(ThemeService);

  /** Si défini (`environment.practitionerDoctorId` ou localStorage `mediagenda-practitioner-doctor-id`), affiche « Mon agenda ». */
  readonly practitionerDoctorId: string | null = (() => {
    const envId = environment.practitionerDoctorId?.trim();
    let ls: string | null = null;
    try {
      ls = typeof localStorage !== 'undefined' ? localStorage.getItem('mediagenda-practitioner-doctor-id') : null;
    } catch {
      ls = null;
    }
    const id = envId || ls?.trim();
    return id || null;
  })();

  readonly modalOpen = signal(false);

  /** Rendez-vous passé au modal en mode édition (`null` pour une création). */
  readonly modalEditAppointment = signal<Appointment | null>(null);

  readonly currentView$ = this.agenda.currentView$;

  readonly mobileSidebarOpen = toSignal(this.agenda.mobileSidebarOpen$, {
    initialValue: false,
  });

  readonly viewItems: ReadonlyArray<{ view: AgendaView; label: string }> = [
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
