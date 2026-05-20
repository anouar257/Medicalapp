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

interface CalendarCell {
  key: string;
  date: Date;
  inMonth: boolean;
}

@Component({
  selector: 'app-sidebar-filter',
  standalone: true,
  imports: [AsyncPipe, RouterLink],
  templateUrl: './sidebar-filter.component.html',
  styleUrls: ['./sidebar-filter.component.scss'],
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
