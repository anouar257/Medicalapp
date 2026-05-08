import { AsyncPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { combineLatest, map } from 'rxjs';

import type { AppointmentType, Doctor } from '../../models/agenda.model';
import { AgendaStateService } from '../../services/agenda-state.service';
import { resolveDoctorPhotoUrl } from '../../utils/media-url';

function addDays(d: Date, n: number): Date {
  const x = new Date(d.getTime());
  x.setDate(x.getDate() + n);
  return x;
}

function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1, 12, 0, 0, 0);
}

function addMonths(d: Date, n: number): Date {
  return new Date(d.getFullYear(), d.getMonth() + n, 1, 12, 0, 0, 0);
}

function sameCalendarDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

type CalendarCell = {
  key: string;
  date: Date;
  inMonth: boolean;
};

@Component({
  selector: 'app-sidebar-filter',
  standalone: true,
  imports: [AsyncPipe, RouterLink],
  template: `
    <aside
      class="flex max-h-none min-h-screen w-[min(90vw,20rem)] shrink-0 flex-col gap-6 overflow-y-auto border-r border-slate-200/80 bg-white p-5 text-slate-900 shadow-sm transition-transform duration-300 ease-out dark:border-slate-700/80 dark:bg-[#1E293B] dark:text-slate-100 max-md:fixed max-md:left-0 max-md:top-0 max-md:z-50 max-md:h-[100dvh] max-md:max-h-[100dvh] max-md:shadow-xl md:relative md:inset-auto md:z-auto md:h-auto md:min-h-screen md:w-[20rem] md:translate-x-0"
      [class.max-md:-translate-x-full]="!drawerOpen()"
      [class.max-md:translate-x-0]="drawerOpen()"
      aria-label="Filtres agenda"
    >
      <div class="shrink-0">
        <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">Cabinet</p>
        <h2 class="mt-0.5 text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100">Agenda</h2>
        <p class="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
          Médecins, types de visite, mini-calendrier — fusionnez plusieurs plannings.
        </p>
      </div>

      @if (vm$ | async; as vm) {
        <!-- 1. Médecins (en haut) -->
        <section
          class="rounded-xl border border-slate-200/80 bg-slate-50/70 p-4 shadow-sm dark:border-slate-700/70 dark:bg-slate-800/40"
        >
          <h3 class="text-sm font-semibold text-slate-900 dark:text-slate-100">Médecins</h3>
          <p class="mt-1 text-[11px] leading-snug text-slate-500 dark:text-slate-400">
            Fusion de plannings — cochez plusieurs agendas.
          </p>
          <ul class="mt-4 flex flex-col gap-2">
            @for (doc of vm.doctors; track doc.id) {
              <li>
                <label
                  class="group flex cursor-pointer items-center gap-3 rounded-xl border border-transparent px-2 py-2 transition hover:border-slate-200 hover:bg-white dark:hover:border-slate-600 dark:hover:bg-slate-700/40"
                >
                  <input
                    class="h-4 w-4 shrink-0 rounded border-slate-300 bg-white text-fuchsia-500 focus:ring-2 focus:ring-fuchsia-500 focus:ring-offset-0 dark:border-slate-500 dark:bg-slate-700"
                    type="checkbox"
                    [checked]="vm.selectedDoctorIds.includes(doc.id)"
                    (change)="setDoctorChecked(doc.id, $any($event.target).checked)"
                  />

                  <!-- Photo médecin (obligatoire — fallback initiales) -->
                  <span
                    class="relative inline-flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full ring-2 ring-white shadow-sm dark:ring-slate-700"
                    [style.backgroundColor]="doc.colorCode"
                  >
                    @if (doc.photoUrl) {
                      <img
                        [src]="resolveDoctorPhotoUrl(doc.photoUrl)"
                        [alt]="doc.name"
                        class="h-full w-full object-cover"
                        loading="lazy"
                        referrerpolicy="no-referrer"
                        (error)="onPhotoError($event)"
                      />
                    } @else {
                      <span class="text-[11px] font-bold text-white">{{ initials(doc.name) }}</span>
                    }
                  </span>

                  <span
                    class="flex min-w-0 flex-1 flex-col border-l-[3px] pl-3 text-sm font-medium text-slate-800 group-hover:text-slate-900 dark:text-slate-100 dark:group-hover:text-white"
                    [style.border-left-color]="doc.colorCode"
                  >
                    <span class="block truncate">{{ doc.name }}</span>
                    @if (doc.specialty) {
                      <span class="block truncate text-[11px] font-normal text-slate-500 dark:text-slate-400">{{
                        doc.specialty
                      }}</span>
                    }
                  </span>
                </label>
              </li>
            }
            @if (vm.doctors.length === 0) {
              <li
                class="rounded-xl border border-dashed border-slate-300/90 bg-white px-4 py-5 text-center dark:border-slate-600 dark:bg-slate-800/50"
              >
                <span
                  class="mx-auto mb-3 flex size-11 items-center justify-center rounded-full bg-slate-100 text-slate-500 dark:bg-slate-700/80 dark:text-slate-400"
                  aria-hidden="true"
                >
                  <svg class="size-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"
                    />
                  </svg>
                </span>
                <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">Aucun médecin listé</p>
                <p class="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  Ajoutez des praticiens depuis l’administration cabinet ou vérifiez que le service agenda répond.
                </p>
                <a
                  routerLink="/admin/doctors"
                  class="mt-4 inline-flex items-center justify-center rounded-full bg-blue-600 px-4 py-2 text-xs font-semibold text-white shadow-sm shadow-blue-600/20 transition hover:bg-blue-500"
                  >Ouvrir l’admin</a
                >
              </li>
            }
          </ul>
        </section>

        <!-- 2. Types de visite (au milieu, dynamiques) -->
        <section
          class="rounded-xl border border-slate-200/80 bg-slate-50/70 p-4 shadow-sm dark:border-slate-700/70 dark:bg-slate-800/40"
        >
          <h3 class="text-sm font-semibold text-slate-900 dark:text-slate-100">Type de visite</h3>
          <p class="mt-1 text-[11px] leading-snug text-slate-500 dark:text-slate-400">
            Filtres du planning — ordre et durées par défaut alignés sur le catalogue cabinet.
          </p>
          <div class="mt-4 flex flex-col gap-2">
            @for (t of vm.appointmentTypes; track t.id) {
              <label
                class="group flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200/90 bg-white px-3 py-2.5 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800/50 dark:hover:bg-slate-700/50"
              >
                <span
                  class="relative inline-flex size-9 shrink-0 items-center justify-center rounded-full ring-2 ring-white dark:ring-slate-700"
                  [style.backgroundColor]="t.colorCode"
                  aria-hidden="true"
                ></span>
                <span class="min-w-0 flex-1">
                  <span class="block text-sm font-semibold leading-tight text-slate-900 dark:text-slate-100">{{
                    t.label
                  }}</span>
                  <span class="mt-0.5 block text-[11px] font-medium tabular-nums text-slate-500 dark:text-slate-400">
                    {{ t.defaultDurationMinutes }} min par défaut
                  </span>
                </span>
                <input
                  type="checkbox"
                  class="h-4 w-4 shrink-0 rounded border-slate-300 bg-white text-emerald-500 focus:ring-2 focus:ring-emerald-400 focus:ring-offset-0 dark:border-slate-500 dark:bg-slate-700"
                  [checked]="vm.visibleTypeCodes.includes(t.code)"
                  (change)="setTypeChecked(t.code, $any($event.target).checked)"
                />
              </label>
            }
            @if (vm.appointmentTypes.length === 0) {
              <div
                class="rounded-xl border border-dashed border-slate-300/90 bg-white px-4 py-5 text-center dark:border-slate-600 dark:bg-slate-800/50"
              >
                <span
                  class="mx-auto mb-3 flex size-11 items-center justify-center rounded-full bg-slate-100 text-slate-500 dark:bg-slate-700/80 dark:text-slate-400"
                  aria-hidden="true"
                >
                  <svg class="size-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5a2.25 2.25 0 002.25-2.25m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5a2.25 2.25 0 012.25 2.25v7.5"
                    />
                  </svg>
                </span>
                <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">Aucun type de visite</p>
                <p class="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  Le catalogue des motifs doit être initialisé (seed ou admin) pour filtrer le planning.
                </p>
                <a
                  routerLink="/admin/doctors"
                  class="mt-4 inline-flex items-center justify-center rounded-full border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                  >Admin cabinet</a
                >
              </div>
            }
          </div>
        </section>

        <!-- 3. Mini-calendrier (en bas) -->
        <section
          class="shrink-0 rounded-xl border border-slate-200/80 bg-slate-50/70 p-4 shadow-sm dark:border-slate-700/70 dark:bg-slate-800/40"
        >
          <div class="flex items-center justify-between gap-2">
            <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Aller au jour
            </h3>
          </div>
          <div class="mt-3 flex items-center justify-between gap-2">
            <button
              type="button"
              class="inline-flex size-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900 focus:outline-none focus:ring-2 focus:ring-fuchsia-500/60 dark:border-slate-600 dark:bg-slate-700/50 dark:text-slate-300 dark:hover:bg-slate-700/80 dark:hover:text-white"
              (click)="stepMonth(-1)"
              aria-label="Mois précédent"
            >
              ‹
            </button>
            <p class="min-w-0 flex-1 truncate text-center text-sm font-semibold capitalize text-slate-900 dark:text-white">
              {{ monthTitle(viewMonth()) }}
            </p>
            <button
              type="button"
              class="inline-flex size-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900 focus:outline-none focus:ring-2 focus:ring-fuchsia-500/60 dark:border-slate-600 dark:bg-slate-700/50 dark:text-slate-300 dark:hover:bg-slate-700/80 dark:hover:text-white"
              (click)="stepMonth(1)"
              aria-label="Mois suivant"
            >
              ›
            </button>
          </div>
          <div
            class="mt-4 grid grid-cols-7 gap-1 text-center text-[10px] font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400"
          >
            @for (wd of weekdays; track wd) {
              <span class="pb-2">{{ wd }}</span>
            }
          </div>
          <div class="grid grid-cols-7 gap-1">
            @for (cell of calendarCells(); track cell.key) {
              <button
                type="button"
                [class]="dayCellClass(cell, vm.selectedDate)"
                (click)="pickDay(cell.date)"
              >
                {{ cell.date.getDate() }}
              </button>
            }
          </div>
        </section>
      }
    </aside>
  `,
})
export class SidebarFilterComponent {
  private readonly agenda = inject(AgendaStateService);

  protected readonly resolveDoctorPhotoUrl = resolveDoctorPhotoUrl;

  /** Drawer mobile : synchro avec `AgendaStateService.mobileSidebarOpen$`. */
  protected readonly drawerOpen = toSignal(this.agenda.mobileSidebarOpen$, {
    initialValue: false,
  });

  protected readonly viewMonth = signal<Date>(startOfMonth(this.agenda.selectedDate));

  readonly vm$ = combineLatest({
    doctors: this.agenda.doctors$,
    selectedDoctorIds: this.agenda.selectedDoctors$,
    appointmentTypes: this.agenda.appointmentTypes$,
    visibleTypeCodes: this.agenda.visibleTypeCodes$,
    selectedDate: this.agenda.selectedDate$,
  }).pipe(
    map(({ doctors, selectedDoctorIds, appointmentTypes, visibleTypeCodes, selectedDate }) => ({
      doctors,
      selectedDoctorIds,
      appointmentTypes: appointmentTypes
        .filter((t) => t.active)
        .slice()
        .sort((a, b) => {
          const o = a.displayOrder - b.displayOrder;
          return o !== 0 ? o : a.label.localeCompare(b.label, 'fr');
        }),
      visibleTypeCodes,
      selectedDate,
    })),
  );

  protected readonly weekdays = ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'];

  private readonly calendarModel = computed(() => {
    const anchor = this.viewMonth();
    const y = anchor.getFullYear();
    const m = anchor.getMonth();
    const first = new Date(y, m, 1, 12, 0, 0, 0);
    const startOffset = (first.getDay() + 6) % 7;
    const start = addDays(first, -startOffset);
    const cells: CalendarCell[] = [];
    for (let i = 0; i < 42; i++) {
      const date = addDays(start, i);
      cells.push({
        key: `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`,
        date,
        inMonth: date.getMonth() === m,
      });
    }
    return cells;
  });

  monthTitle(d: Date): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(d);
  }

  /** Initiales (max 2) — fallback affichage sans photo. */
  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');
  }

  onPhotoError(ev: Event): void {
    const img = ev.target as HTMLImageElement | null;
    if (img) {
      img.style.display = 'none';
    }
  }

  /** Classes Tailwind regroupées pour éviter les bindings invalides en template. */
  dayCellClass(cell: CalendarCell, selectedDate: Date): string {
    const base =
      'aspect-square rounded-xl text-xs font-semibold transition focus:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500/80 ';
    const sel = this.sameDay(cell.date, selectedDate);
    const isToday = this.today(cell.date);
    if (!cell.inMonth) {
      return base + 'opacity-40 text-slate-400 dark:text-slate-600';
    }
    if (sel) {
      return base + 'bg-fuchsia-500 text-white shadow-lg ring-2 ring-fuchsia-400';
    }
    if (isToday) {
      return base + 'bg-violet-500/20 text-slate-800 ring-1 ring-violet-400/60 dark:bg-violet-500/30 dark:text-slate-100';
    }
    return (
      base +
      'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-slate-100 dark:bg-slate-700/40 dark:text-slate-200 dark:ring-slate-600/60 dark:hover:bg-slate-700/70'
    );
  }

  calendarCells(): CalendarCell[] {
    return this.calendarModel();
  }

  sameDay(a: Date, b: Date): boolean {
    return sameCalendarDay(a, b);
  }

  today(d: Date): boolean {
    return sameCalendarDay(d, new Date());
  }

  stepMonth(delta: number): void {
    this.viewMonth.update((m) => addMonths(m, delta));
  }

  pickDay(day: Date): void {
    const normalized = new Date(day.getFullYear(), day.getMonth(), day.getDate(), 12, 0, 0, 0);
    this.agenda.setSelectedDate(normalized);
    this.agenda.closeMobileSidebar();
  }

  setDoctorChecked(id: string, checked: boolean): void {
    const next = new Set(this.agenda.selectedDoctorIds);
    if (checked) {
      next.add(id);
    } else {
      next.delete(id);
    }
    this.agenda.setSelectedDoctorIds([...next]);
  }

  setTypeChecked(code: string, checked: boolean): void {
    const next = new Set(this.agenda.visibleTypeCodes);
    if (checked) {
      next.add(code);
    } else {
      next.delete(code);
    }
    this.agenda.setVisibleTypeCodes([...next]);
  }
}
