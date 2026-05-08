import { DragDropModule, CdkDragEnd, CdkDragMove } from '@angular/cdk/drag-drop';
import { AsyncPipe, NgClass, DOCUMENT } from '@angular/common';
import { Component, inject, output, Renderer2 } from '@angular/core';
import { combineLatest, map } from 'rxjs';

import type { AgendaView, Appointment, AppointmentStatus, Doctor } from '../../models/agenda.model';
import type { GridComfortMode } from '../../services/agenda-state.service';
import { AgendaStateService } from '../../services/agenda-state.service';
import { ThemeService } from '../../services/theme.service';
import { resolveDoctorPhotoUrl } from '../../utils/media-url';

/** Fallback si `Appointment.color` est absent (API / anciennes données). */
const DEFAULT_APPOINTMENT_HEX = '#3B82F6';

const CALENDAR_MATRIX_WEEKDAYS = ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'] as const;

/** RDV visibles par jour en vue mois / mini-mois année. */
const MAX_CHIPS_MONTH_VIEW = 5;
const MAX_CHIPS_YEAR_MINI_DAY = 2;

/** Fenêtre affichée : 08:00 → 19:00 (11 h = 660 min). */
const ANCHOR_HOUR = 8;
const END_HOUR = 19;
const HOURS_SPAN = END_HOUR - ANCHOR_HOUR;
const TOTAL_MINUTES = HOURS_SPAN * 60;

/**
 * Hauteur de la timeline : 280 px/h (confort) ou 140 px/h (compact).
 * Très courts RDV : hauteur d’affichage ≥ 1 créneau de 15 min (lisibilité), le haut de carte reste à l’heure de début.
 * Ex. confort : 15 min ≈ 70 px par créneau quart d’heure.
 */
function gridBodyHeightPx(mode: GridComfortMode): number {
  const pxPerHour = mode === 'comfortable' ? 280 : 140;
  return HOURS_SPAN * pxPerHour;
}

/** 15 minutes en ms — référence pour hauteur minimale lisible et pour les lanes. */
const FIFTEEN_MIN_MS = 15 * 60 * 1000;

/**
 * Hauteur d’un créneau de 15 min sur la timeline (les RDV plus courts utilisent au moins cette hauteur
 * pour afficher badge + horaire + titre + médecin, tout en gardant le haut de carte aligné sur l’heure exacte).
 */
function oneQuarterSlotHeightPx(bodyHeightPx: number): number {
  return (15 / TOTAL_MINUTES) * bodyHeightPx;
}

function startOfDay(d: Date): Date {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function endExclusiveDay(d: Date): Date {
  const x = startOfDay(d);
  x.setDate(x.getDate() + 1);
  return x;
}

function addDays(d: Date, days: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + days);
  return x;
}

function noonDate(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 12, 0, 0, 0);
}

function getViewRange(
  selectedDate: Date,
  view: AgendaView,
  opts?: { dayShowsThreeDays?: boolean },
): { start: Date; endExclusive: Date } {
  const ref = selectedDate;

  switch (view) {
    case 'day': {
      if (opts?.dayShowsThreeDays) {
        const start = startOfDay(ref);
        return { start, endExclusive: addDays(start, 3) };
      }
      return { start: startOfDay(ref), endExclusive: endExclusiveDay(ref) };
    }
    case 'week': {
      const start = startOfDay(ref);
      const jsDay = start.getDay();
      const deltaToMonday = jsDay === 0 ? -6 : 1 - jsDay;
      start.setDate(start.getDate() + deltaToMonday);
      return { start, endExclusive: addDays(start, 7) };
    }
    case 'month': {
      const start = new Date(ref.getFullYear(), ref.getMonth(), 1);
      const endExclusive = new Date(ref.getFullYear(), ref.getMonth() + 1, 1);
      return { start, endExclusive };
    }
    case 'year': {
      const start = new Date(ref.getFullYear(), 0, 1);
      const endExclusive = new Date(ref.getFullYear() + 1, 0, 1);
      return { start, endExclusive };
    }
  }
}

function enumerateColumnDays(
  selectedDate: Date,
  view: AgendaView,
  dayShowsThreeDays?: boolean,
): Date[] {
  const { start, endExclusive } = getViewRange(selectedDate, view, {
    dayShowsThreeDays: view === 'day' ? dayShowsThreeDays : false,
  });
  const days: Date[] = [];
  for (let cursor = startOfDay(start); cursor.getTime() < endExclusive.getTime(); cursor = addDays(cursor, 1)) {
    days.push(new Date(cursor.getTime()));
  }
  return days;
}

function sameCalendarDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function minutesFromAnchor(d: Date): number {
  return d.getHours() * 60 + d.getMinutes() + d.getSeconds() / 60 - ANCHOR_HOUR * 60;
}

function clampMinutesToGrid(m: number): number {
  if (Number.isNaN(m)) return 0;
  return Math.max(0, Math.min(TOTAL_MINUTES, m));
}

type TimeAxisTickVm = {
  key: string;
  topPct: number;
  label: string;
  tier: 'hour' | 'quarter';
};

function buildQuarterAxisTicks(): TimeAxisTickVm[] {
  const ticks: TimeAxisTickVm[] = [];
  for (let m = 0; m <= TOTAL_MINUTES; m += 15) {
    const totalMinDay = ANCHOR_HOUR * 60 + m;
    const hh24 = Math.floor(totalMinDay / 60);
    const mmPart = totalMinDay % 60;
    const isHour = mmPart === 0;
    ticks.push({
      key: `q-${m}`,
      topPct: (m / TOTAL_MINUTES) * 100,
      label: `${String(hh24).padStart(2, '0')}:${String(mmPart).padStart(2, '0')}`,
      tier: isHour ? 'hour' : 'quarter',
    });
  }
  return ticks;
}

type ColumnVm = {
  key: string;
  header: string;
  sub: string;
  appointments: PlacedAppointment[];
};

type PlacedAppointment = {
  id: string;
  title: string;
  doctorName: string;
  /** Photo médecin (URL absolue ou `/assets/...` depuis l’API). */
  doctorPhotoUrl: string;
  doctorColorCode: string;
  typeLabel: string;
  typeColor: string;
  startLabel: string;
  endLabel: string;
  /** Heure de début seule — ligne principale type maquette (ex. « 11:30 »). */
  startDisplayLabel: string;
  /** Ex. « 09:30 – 09:35 » — sous-ligne ou aria (durée réelle ; hauteur carte peut être ≥ pour lisibilité). */
  timeRangeLabel: string;
  topPx: number;
  heightPx: number;
  leftPct: number;
  widthPct: number;
  color: string;
  durationMinutes: number;
  startTimeMs: number;
  status?: AppointmentStatus;
};

type MatrixAppointmentChipVm = {
  id: string;
  title: string;
  startLabel: string;
  doctorName: string;
  color: string;
  typeLabel: string;
  typeColor: string;
  status?: AppointmentStatus;
};

type MonthMatrixCellVm = {
  key: string;
  date: Date;
  belongsToDisplayedMonth: boolean;
  isSelected: boolean;
  isToday: boolean;
  chips: MatrixAppointmentChipVm[];
  overflowCount: number;
};

type YearMiniMonthVm = {
  monthIndex: number;
  label: string;
  weekRows: MonthMatrixCellVm[][];
};

type CalendarLayoutMode = 'timeline' | 'monthMatrix' | 'yearMatrix';

type CalendarVm = {
  layoutMode: CalendarLayoutMode;
  view: AgendaView;
  title: string;
  viewMeta: string;
  calendarSubtitle: string;
  timelineHasEvents?: boolean;
  dayShowsThreeDays?: boolean;
  columnDays?: Date[];
  columns?: ColumnVm[];
  quarterTicks?: TimeAxisTickVm[];
  gridTemplateColumns?: string;
  bodyHeightPx?: number;
  gridComfort?: GridComfortMode;
  monthWeekRows?: MonthMatrixCellVm[][];
  yearMiniMonths?: YearMiniMonthVm[];
};

function formatTime(d: Date): string {
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(d);
}

/** Minutes depuis minuit (nombre arrondi) → `HH:mm`. */
function formatTimeFromMinutesSinceMidnight(totalMinutes: number): string {
  const m = Math.round(totalMinutes);
  const h = Math.floor(m / 60) % 24;
  const min = m % 60;
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
}

function buildAppointmentChipsForDay(
  day: Date,
  appointments: Appointment[],
  maxChips: number,
  doctorsById: Map<string, Doctor>,
): { chips: MatrixAppointmentChipVm[]; overflowCount: number } {
  const list = appointments
    .filter((a) => sameCalendarDay(a.startTime, day))
    .sort((x, y) => x.startTime.getTime() - y.startTime.getTime());
  const chips: MatrixAppointmentChipVm[] = list.slice(0, maxChips).map((a) => ({
    id: a.id,
    title: a.title,
    startLabel: formatTime(a.startTime),
    doctorName: doctorsById.get(a.doctorId)?.name ?? 'Médecin inconnu',
    color: (a.color ?? '').trim() || DEFAULT_APPOINTMENT_HEX,
    typeLabel: a.typeLabel ?? a.typeCode ?? '—',
    typeColor: (a.typeColor ?? '').trim() || DEFAULT_APPOINTMENT_HEX,
    status: a.status,
  }));
  return { chips, overflowCount: Math.max(0, list.length - maxChips) };
}

function weekRowsForCalendarMonth(
  year: number,
  monthIndex: number,
  appointments: Appointment[],
  selectedDate: Date,
  maxChipsPerDay: number,
  doctorsById: Map<string, Doctor>,
): MonthMatrixCellVm[][] {
  const weeks: MonthMatrixCellVm[][] = [];
  const first = new Date(year, monthIndex, 1, 12, 0, 0, 0);
  const startOffset = (first.getDay() + 6) % 7;
  let cursor = addDays(first, -startOffset);
  const today = new Date();

  for (let w = 0; w < 6; w++) {
    const row: MonthMatrixCellVm[] = [];
    for (let d = 0; d < 7; d++) {
      const belongsToDisplayedMonth = cursor.getMonth() === monthIndex && cursor.getFullYear() === year;
      const { chips, overflowCount } = buildAppointmentChipsForDay(
        cursor,
        appointments,
        maxChipsPerDay,
        doctorsById,
      );
      row.push({
        key: `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`,
        date: new Date(cursor),
        belongsToDisplayedMonth,
        isSelected: sameCalendarDay(cursor, selectedDate),
        isToday: sameCalendarDay(cursor, today),
        chips,
        overflowCount,
      });
      cursor = addDays(cursor, 1);
    }
    weeks.push(row);
  }
  return weeks;
}

function assignLanes(dayAppointments: Appointment[]): Map<string, { lane: number; lanesTotal: number }> {
  const sorted = dayAppointments.slice().sort((a, b) => {
    const t = a.startTime.getTime() - b.startTime.getTime();
    if (t !== 0) {
      return t;
    }
    const te = a.endTime.getTime() - b.endTime.getTime();
    if (te !== 0) {
      return te;
    }
    return a.id.localeCompare(b.id);
  });

  const out = new Map<string, { lane: number; lanesTotal: number }>();

  let currentCluster: Appointment[] = [];
  let clusterEndMs = 0;

  const processCluster = (cluster: Appointment[]) => {
    if (cluster.length === 0) return;
    const laneEnds: number[] = [];
    const laneById = new Map<string, number>();

    for (const apt of cluster) {
      const durationMs = apt.endTime.getTime() - apt.startTime.getTime();
      const visualEndMs = apt.startTime.getTime() + Math.max(durationMs, FIFTEEN_MIN_MS);

      let lane = laneEnds.findIndex((endMs) => endMs <= apt.startTime.getTime());
      if (lane === -1) {
        lane = laneEnds.length;
        laneEnds.push(visualEndMs);
      } else {
        laneEnds[lane] = Math.max(laneEnds[lane], visualEndMs);
      }
      laneById.set(apt.id, lane);
    }

    const lanesTotal = Math.max(1, laneEnds.length);
    for (const apt of cluster) {
      out.set(apt.id, { lane: laneById.get(apt.id) ?? 0, lanesTotal });
    }
  };

  for (const apt of sorted) {
    const durationMs = apt.endTime.getTime() - apt.startTime.getTime();
    const visualEndMs = apt.startTime.getTime() + Math.max(durationMs, FIFTEEN_MIN_MS);

    if (currentCluster.length === 0) {
      currentCluster.push(apt);
      clusterEndMs = visualEndMs;
    } else {
      if (apt.startTime.getTime() >= clusterEndMs) {
        processCluster(currentCluster);
        currentCluster = [apt];
        clusterEndMs = visualEndMs;
      } else {
        currentCluster.push(apt);
        clusterEndMs = Math.max(clusterEndMs, visualEndMs);
      }
    }
  }
  processCluster(currentCluster);

  return out;
}

function placeAppointmentsForColumn(
  columnDay: Date,
  appointments: Appointment[],
  doctorsById: Map<string, Doctor>,
  bodyHeightPx: number,
): PlacedAppointment[] {
  const dayApps = appointments
    .filter((a) => sameCalendarDay(a.startTime, columnDay))
    .slice()
    .sort((a, b) => {
      const t = a.startTime.getTime() - b.startTime.getTime();
      if (t !== 0) {
        return t;
      }
      const te = a.endTime.getTime() - b.endTime.getTime();
      if (te !== 0) {
        return te;
      }
      return a.id.localeCompare(b.id);
    });

  const lanes = assignLanes(dayApps);
  const laneColumnCount = dayApps.length > 0 ? (lanes.get(dayApps[0].id)?.lanesTotal ?? 1) : 1;
  const gapPct = laneColumnCount > 1 ? 1.8 : 1.2;

  return dayApps
    .map((a) => {
      const s = minutesFromAnchor(a.startTime);
      const e = minutesFromAnchor(a.endTime);
      const visS = clampMinutesToGrid(s);
      const visE = clampMinutesToGrid(e);
      if (visE <= visS) {
        return null;
      }

      const rawHeightPx = ((visE - visS) / TOTAL_MINUTES) * bodyHeightPx;
      const topPx = (visS / TOTAL_MINUTES) * bodyHeightPx;
      const minReadablePx = oneQuarterSlotHeightPx(bodyHeightPx);
      const heightPx = Math.max(rawHeightPx, minReadablePx);

      const laneInfo = lanes.get(a.id)!;
      const { lane, lanesTotal } = laneInfo;
      const segment = 100 / lanesTotal;
      const leftPct = lane * segment + gapPct / 2;
      const widthPct = segment - gapPct;

      const doctor = doctorsById.get(a.doctorId);
      const doctorName = doctor?.name ?? 'Médecin inconnu';
      const doctorPhotoUrl = (doctor?.photoUrl ?? '').trim();
      const doctorColorCode = doctor?.colorCode ?? '#64748b';
      const colorRaw = (a.color ?? '').trim();
      const color = colorRaw || DEFAULT_APPOINTMENT_HEX;

      const startLabel = formatTime(a.startTime);
      const endLabel = formatTime(a.endTime);
      const timeRangeLabel = `${startLabel} – ${endLabel}`;
      const startDisplayLabel = startLabel;

      return {
        id: a.id,
        title: a.title,
        doctorName,
        doctorPhotoUrl,
        doctorColorCode,
        typeLabel: a.typeLabel ?? a.typeCode ?? '—',
        typeColor: (a.typeColor ?? '').trim() || DEFAULT_APPOINTMENT_HEX,
        startLabel,
        endLabel,
        startDisplayLabel,
        timeRangeLabel,
        topPx,
        heightPx,
        leftPct,
        widthPct,
        color,
        durationMinutes: a.durationMinutes,
        startTimeMs: a.startTime.getTime(),
        status: a.status,
      };
    })
    .filter(Boolean) as PlacedAppointment[];
}

function dateFromColumnKey(key: string): Date {
  const [y, m, d] = key.split('-').map((x) => parseInt(x, 10));
  return new Date(y, m, d, 0, 0, 0, 0);
}

const VIEW_HINTS: Record<AgendaView, string> = {
  day: 'Vue jour',
  week: 'Vue semaine (lundi → dimanche)',
  month: 'Vue mois — grille calendrier (lun. → dim.)',
  year: "Vue année — 12 mois d'affilée",
};

@Component({
  selector: 'app-calendar-grid',
  standalone: true,
  imports: [AsyncPipe, NgClass, DragDropModule],
  template: `
    @if (vm$ | async; as vm) {
      <section
        class="calendar-dashboard-panel flex min-w-0 flex-col gap-4 rounded-xl border border-slate-200/90 bg-white p-5 shadow-sm dark:border-slate-700/80 dark:bg-[#1E293B] sm:p-6"
        aria-label="Grille agenda"
      >
        <header
          class="flex flex-wrap items-end justify-between gap-3 border-b border-slate-100 pb-4 dark:border-slate-700/80"
        >
          <div class="flex min-w-0 flex-1 flex-wrap items-end gap-x-6 gap-y-3">
            <div class="min-w-0">
              <p class="text-[11px] font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Planning
              </p>
              <h2 class="capitalize text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
                {{ vm.title }}
              </h2>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ vm.calendarSubtitle }}</p>
            </div>
            @if (vm.layoutMode === 'timeline' && vm.view === 'day') {
              <div
                class="inline-flex shrink-0 items-center gap-0.5 rounded-full bg-slate-100 p-1 ring-1 ring-slate-200/90 dark:bg-slate-800/90 dark:ring-slate-600/80"
                role="group"
                aria-label="Étendre le jour sur trois colonnes ou une seule"
              >
                <button
                  type="button"
                  class="rounded-full px-3 py-2 text-xs font-semibold transition hover:bg-white hover:text-slate-900 dark:hover:bg-slate-700"
                  [class.bg-white]="!vm.dayShowsThreeDays"
                  [class.shadow-sm]="!vm.dayShowsThreeDays"
                  [class.text-slate-900]="!vm.dayShowsThreeDays"
                  [class.text-slate-600]="vm.dayShowsThreeDays"
                  [class.dark:bg-slate-600]="!vm.dayShowsThreeDays"
                  [class.dark:text-slate-100]="!vm.dayShowsThreeDays"
                  [class.dark:text-slate-400]="vm.dayShowsThreeDays"
                  (click)="setDayShowsThreeDays(false)"
                >
                  1 jour
                </button>
                <button
                  type="button"
                  class="rounded-full px-3 py-2 text-xs font-semibold transition hover:bg-white hover:text-slate-900 dark:hover:bg-slate-700"
                  [class.bg-white]="vm.dayShowsThreeDays"
                  [class.shadow-sm]="vm.dayShowsThreeDays"
                  [class.text-slate-900]="vm.dayShowsThreeDays"
                  [class.text-slate-600]="!vm.dayShowsThreeDays"
                  [class.dark:bg-slate-600]="vm.dayShowsThreeDays"
                  [class.dark:text-slate-100]="vm.dayShowsThreeDays"
                  [class.dark:text-slate-400]="!vm.dayShowsThreeDays"
                  (click)="setDayShowsThreeDays(true)"
                >
                  3 jours
                </button>
              </div>
            }
          </div>
          <div
            class="max-w-[12rem] text-right text-[11px] leading-snug text-slate-400 dark:text-slate-500 sm:max-w-none sm:text-xs"
          >
            {{ vm.viewMeta }}
          </div>
        </header>

        <div class="hidden sm:block">
        @if (vm.layoutMode === 'timeline' && !vm.timelineHasEvents) {
          <div
            class="flex flex-col items-center rounded-xl border border-dashed border-slate-200 bg-gradient-to-b from-white to-slate-50/80 px-6 py-14 text-center shadow-sm dark:border-slate-600 dark:from-[#1E293B] dark:to-slate-900/40"
            role="status"
          >
            <span
              class="mb-4 flex size-14 items-center justify-center rounded-2xl bg-blue-600/10 text-blue-600 ring-1 ring-blue-600/20 dark:bg-blue-500/15 dark:text-blue-400 dark:ring-blue-400/25"
              aria-hidden="true"
            >
              <svg class="size-8" fill="none" stroke="currentColor" stroke-width="1.6" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5a2.25 2.25 0 002.25-2.25m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5a2.25 2.25 0 012.25 2.25v7.5"
                />
              </svg>
            </span>
            <p class="text-base font-semibold text-slate-800 dark:text-slate-100">Aucun rendez-vous affiché</p>
            <p class="mt-2 max-w-md text-sm leading-relaxed text-slate-500 dark:text-slate-400">
              Changez la date, élargissez les médecins cochés ou les types de visite dans la colonne de gauche.
            </p>
          </div>
        } @else if (vm.layoutMode === 'timeline') {
          <div
            class="-mx-1 max-h-[min(78vh,1100px)] overflow-auto rounded-xl border border-slate-100 bg-slate-50 scrollbar-calendar shadow-inner dark:border-slate-700/70 dark:bg-slate-900/40"
          >
            <div class="flex min-w-max">
              <div
                class="sticky left-0 z-40 w-[min(6.5rem,16vw)] shrink-0 rounded-l-xl border-r border-slate-100 bg-white shadow-md dark:border-slate-700/80 dark:bg-[#1E293B]"
              >
                <div
                  class="h-[3.25rem] shrink-0 border-b border-slate-100 bg-slate-50/90 dark:border-slate-700/80 dark:bg-slate-800/60"
                ></div>
                <div class="relative bg-white dark:bg-[#1E293B]" [style.height.px]="vm.bodyHeightPx">
                  @for (tick of vm.quarterTicks; track tick.key) {
                    <div
                      class="pointer-events-none absolute left-0 right-0 pr-1"
                      [ngClass]="timelineTickLineNgClass(tick)"
                      [style.top.%]="tick.topPct"
                    >
                      <span
                        class="inline-block min-w-[3.75rem] -translate-y-1/2 bg-white px-1 text-right dark:bg-[#1E293B]"
                        [ngClass]="timelineTickLabelNgClass(tick)"
                        >{{ tick.label }}</span
                      >
                    </div>
                  }
                </div>
              </div>

              <div class="calendar-drag-boundary min-w-0 flex-1 overflow-x-auto overscroll-x-contain">
                <div class="grid min-w-max" [style.gridTemplateColumns]="vm.gridTemplateColumns">
                  @for (col of vm.columns; track col.key) {
                    <div
                      class="mx-px min-w-0 overflow-hidden rounded-lg border border-slate-200/80 bg-white shadow-[8px_0_28px_-16px_rgba(15,23,42,0.28)] ring-1 ring-slate-900/5 transition-shadow duration-300 hover:shadow-[10px_0_32px_-14px_rgba(15,23,42,0.35)] dark:border-slate-600/70 dark:bg-[#1E293B] dark:shadow-[10px_0_36px_-18px_rgba(0,0,0,0.62)] dark:ring-white/10 dark:hover:shadow-[12px_0_42px_-16px_rgba(0,0,0,0.75)] sm:rounded-xl"
                    >
                      <div
                        class="flex h-[3.25rem] flex-col items-center justify-center border-b border-slate-100 bg-gradient-to-b from-white to-slate-50/90 px-2 text-center shadow-inner dark:border-slate-700/80 dark:from-[#1E293B] dark:to-[#162032]"
                      >
                        <span
                          class="text-[11px] font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500"
                          >{{ col.sub }}</span
                        >
                        <span class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{
                          col.header
                        }}</span>
                      </div>
                      <div
                        class="calendar-drop-zone relative border-b border-slate-50 bg-gradient-to-b from-white via-white to-slate-50/[0.35] shadow-inner dark:border-slate-800/50 dark:from-[#1E293B] dark:via-[#19273a] dark:to-[#141c2b]"
                        [attr.data-drop-col]="col.key"
                        [style.height.px]="vm.bodyHeightPx"
                      >
                        @for (tick of vm.quarterTicks; track tick.key) {
                          <div
                            class="pointer-events-none absolute inset-x-0"
                            [ngClass]="timelineTickColumnLineNgClass(tick)"
                            [style.top.%]="tick.topPct"
                          ></div>
                        }

                        @for (apt of col.appointments; track apt.id) {
                          <article
                            cdkDrag
                            [cdkDragBoundary]="'.calendar-drag-boundary'"
                            [cdkDragData]="apt"
                            (cdkDragStarted)="onDragStarted(apt)"
                            (cdkDragMoved)="onDragMoved($event, apt, vm.bodyHeightPx || 0)"
                            (cdkDragEnded)="onAppointmentDragEnded($event, apt)"
                            (click)="onAppointmentCardClick($event, apt.id)"
                            tabindex="0"
                            (keydown.enter)="openAppointmentKeyboard(apt.id, $event)"
                            (keydown.space)="openAppointmentKeyboard(apt.id, $event)"
                            role="button"
                            class="event-card group absolute flex cursor-grab flex-col justify-start gap-1 overflow-y-auto overflow-x-hidden rounded-lg border border-slate-200/90 px-3 py-2.5 text-left text-sm shadow-sm ring-1 ring-slate-900/5 backdrop-blur-sm transition-all hover:z-40 hover:shadow-md active:cursor-grabbing focus-within:z-40 focus-visible:z-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50 focus-visible:ring-offset-2 focus-visible:ring-offset-white dark:border-slate-600/55 dark:ring-white/10 dark:focus-visible:ring-sky-400/40 dark:focus-visible:ring-offset-[#1E293B]"
                            [ngClass]="appointmentCardStatusNgClass(apt)"
                            [style.backgroundColor]="hexToRgba(apt.color, cardBackgroundAlpha())"
                            [style.borderLeftWidth.px]="4"
                            [style.borderLeftStyle]="'solid'"
                            [style.borderLeftColor]="resolveAppointmentHex(apt.color)"
                            [style.top.px]="apt.topPx"
                            [style.height.px]="apt.heightPx"
                            [style.left.%]="apt.leftPct"
                            [style.width.%]="apt.widthPct"
                            [attr.aria-label]="apt.title + ' — ' + apt.doctorName + ' — ' + apt.timeRangeLabel"
                          >
                            <div class="flex shrink-0 flex-wrap items-center gap-0.5">
                              <span
                                class="inline-flex max-w-full items-center rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide text-white shadow-sm sm:text-[10px]"
                                [style.backgroundColor]="resolveAppointmentHex(apt.typeColor)"
                              >
                                {{ apt.typeLabel }}
                              </span>
                            </div>
                            <p
                              class="shrink-0 text-[15px] font-bold tabular-nums leading-none tracking-tight text-slate-900 dark:text-slate-50"
                            >
                              {{ appointmentCardPrimaryTime(apt) }}
                            </p>
                            @if (!draggingTimes[apt.id]) {
                              <p
                                class="shrink-0 text-[10px] font-medium tabular-nums leading-tight text-slate-500 dark:text-slate-400"
                              >
                                {{ apt.timeRangeLabel }}
                              </p>
                            }
                            <p
                              class="line-clamp-[6] shrink-0 text-[13px] font-semibold leading-snug text-slate-900 dark:text-slate-100"
                              [class.line-through]="apt.status === 'CANCELLED'"
                            >
                              {{ apt.title }}
                            </p>
                            <div class="flex min-h-0 shrink-0 items-start gap-2 pt-0.5">
                              <img
                                class="size-9 shrink-0 rounded-full object-cover ring-1 ring-slate-200/90 dark:ring-slate-600"
                                [src]="doctorPortraitSrc(apt)"
                                width="36"
                                height="36"
                                alt=""
                              />
                              <span
                                class="line-clamp-3 min-w-0 flex-1 text-[11px] font-medium leading-snug text-slate-500 dark:text-slate-400"
                              >
                                {{ apt.doctorName }}
                              </span>
                            </div>

                            <div
                              class="absolute bottom-0 left-0 right-0 h-2 cursor-ns-resize hover:bg-slate-900/10 dark:hover:bg-white/10"
                              (mousedown)="onResizeStart($event, apt, vm.bodyHeightPx || 0)"
                              (touchstart)="onResizeStart($event, apt, vm.bodyHeightPx || 0)"
                            ></div>
                          </article>
                        }
                      </div>
                    </div>
                  }
                </div>
              </div>
            </div>
          </div>
        } @else if (vm.layoutMode === 'monthMatrix' && vm.monthWeekRows) {
          <div
            class="-mx-1 max-h-[min(82vh,920px)] overflow-y-auto rounded-xl border border-slate-100 bg-slate-50/90 p-3 shadow-inner scrollbar-calendar dark:border-slate-700/70 dark:bg-slate-900/35 sm:p-4"
          >
            <div
              class="grid grid-cols-7 gap-1 border-b border-slate-200 pb-2 dark:border-slate-600 sm:gap-1.5"
            >
              @for (wd of matrixWeekdays; track wd) {
                <div
                  class="text-center text-[10px] font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 sm:text-[11px]"
                >
                  {{ wd }}
                </div>
              }
            </div>
            <div class="grid grid-cols-7 gap-1 pt-2 sm:gap-1.5">
              @for (row of vm.monthWeekRows; track $index) {
                @for (cell of row; track cell.key) {
                  <div
                    class="group flex min-h-[5.5rem] flex-col rounded-lg border p-1 transition sm:min-h-[7rem] sm:p-1.5"
                    [ngClass]="monthMatrixCellNgClass(cell)"
                    role="button"
                    tabindex="0"
                    [attr.aria-label]="'Jour ' + cell.date.getDate()"
                    (click)="onMatrixCellClick($event, cell)"
                    (keydown.enter)="selectMatrixDay(cell.date)"
                    (keydown.space)="selectMatrixDay(cell.date); $event.preventDefault()"
                  >
                    <div class="mb-0.5 flex justify-end">
                      <span
                        class="flex size-6 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold tabular-nums sm:size-7 sm:text-xs"
                        [ngClass]="monthMatrixDayNumberNgClass(cell)"
                      >
                        {{ cell.date.getDate() }}
                      </span>
                    </div>
                    <div class="flex min-h-0 flex-1 flex-col gap-0.5 overflow-hidden">
                      @for (chip of cell.chips; track chip.id) {
                        <button
                          type="button"
                          data-matrix-chip
                          class="matrix-appt-chip flex w-full min-w-0 cursor-pointer flex-col gap-0.5 rounded-md border border-slate-200/75 px-1.5 py-1 text-left shadow-sm ring-1 ring-slate-900/5 transition hover:shadow-md focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:border-slate-600/55 dark:ring-white/10"
                          [ngClass]="matrixChipStatusNgClass(chip)"
                          [style.backgroundColor]="matrixChipBg(chip.color)"
                          [style.borderLeftWidth.px]="3"
                          [style.borderLeftStyle]="'solid'"
                          [style.borderLeftColor]="resolveAppointmentHex(chip.color)"
                          [attr.title]="chip.startLabel + ' — ' + chip.title + ' — ' + chip.doctorName"
                          (click)="$event.stopPropagation(); onAppointmentChipClick(chip.id)"
                        >
                          <div class="flex shrink-0 flex-wrap items-center gap-0.5">
                            <span
                              class="inline-flex max-w-full items-center rounded-full px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wide text-white shadow-sm sm:text-[9px]"
                              [style.backgroundColor]="resolveAppointmentHex(chip.typeColor)"
                            >
                              {{ chip.typeLabel }}
                            </span>
                          </div>
                          <p
                            class="line-clamp-2 text-[10px] font-semibold leading-snug text-slate-800 dark:text-slate-100"
                          >
                            {{ chip.title }}
                          </p>
                          <p
                            class="line-clamp-2 text-[9px] font-medium leading-snug text-slate-600 dark:text-slate-400"
                          >
                            {{ chip.doctorName }}
                          </p>
                        </button>
                      }
                      @if (cell.overflowCount > 0) {
                        <span class="pl-0.5 text-[10px] font-semibold text-slate-500 dark:text-slate-400">
                          +{{ cell.overflowCount }}
                          @if (cell.overflowCount > 1) {
                            autres
                          } @else {
                            autre
                          }
                        </span>
                      }
                    </div>
                  </div>
                }
              }
            </div>
          </div>
        } @else if (vm.layoutMode === 'yearMatrix' && vm.yearMiniMonths) {
          <div
            class="-mx-1 max-h-[min(88vh,1024px)] overflow-y-auto rounded-xl border border-slate-100 bg-slate-50/90 p-3 shadow-inner scrollbar-calendar dark:border-slate-700/70 dark:bg-slate-900/35 sm:p-4"
          >
            <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              @for (mm of vm.yearMiniMonths; track mm.monthIndex) {
                <div
                  class="rounded-xl border border-slate-200/90 bg-white/90 p-2.5 shadow-sm dark:border-slate-600/70 dark:bg-slate-800/50 sm:p-3"
                >
                  <h3 class="mb-2 text-center text-sm font-semibold capitalize text-slate-800 dark:text-slate-100">
                    {{ mm.label }}
                  </h3>
                  <div class="grid grid-cols-7 gap-px border-b border-slate-200 pb-1 dark:border-slate-600">
                    @for (wd of matrixWeekdays; track wd) {
                      <div
                        class="text-center text-[8px] font-semibold uppercase text-slate-400 dark:text-slate-500"
                      >
                        {{ wd }}
                      </div>
                    }
                  </div>
                  <div class="grid grid-cols-7 gap-px pt-1">
                    @for (row of mm.weekRows; track $index) {
                      @for (cell of row; track cell.key) {
                        <div
                          class="flex min-h-[2.35rem] flex-col rounded border border-transparent p-0.5 sm:min-h-[2.6rem]"
                          [ngClass]="yearMiniCellNgClass(cell)"
                          role="button"
                          tabindex="0"
                          (click)="onMatrixCellClick($event, cell)"
                          (keydown.enter)="selectMatrixDay(cell.date)"
                          (keydown.space)="selectMatrixDay(cell.date); $event.preventDefault()"
                        >
                          <span
                            class="block text-right text-[9px] font-semibold tabular-nums leading-none"
                            [ngClass]="yearMiniDayNumberNgClass(cell)"
                          >
                            {{ cell.date.getDate() }}
                          </span>
                          <div class="mt-0.5 flex min-h-0 flex-col gap-px">
                            @for (chip of cell.chips; track chip.id) {
                              <button
                                type="button"
                                data-matrix-chip
                                class="flex max-w-full cursor-pointer flex-col gap-0.5 truncate rounded-sm border border-slate-200/60 border-l-[3px] px-0.5 py-0.5 text-left shadow-sm ring-1 ring-slate-900/5 dark:border-slate-600/50 dark:ring-white/10"
                                [ngClass]="matrixChipStatusNgClass(chip)"
                                [style.backgroundColor]="matrixChipBg(chip.color, true)"
                                [style.borderLeftColor]="resolveAppointmentHex(chip.color)"
                                [attr.title]="chip.startLabel + ' — ' + chip.title + ' — ' + chip.doctorName"
                                (click)="$event.stopPropagation(); onAppointmentChipClick(chip.id)"
                              >
                                <span
                                  class="inline-flex w-fit max-w-full shrink truncate rounded-full px-1 py-px text-[7px] font-bold uppercase tracking-wide text-white"
                                  [style.backgroundColor]="resolveAppointmentHex(chip.typeColor)"
                                >
                                  {{ chip.typeLabel }}
                                </span>
                                <span class="line-clamp-1 text-[7px] font-semibold leading-tight text-slate-800 dark:text-slate-100">{{ chip.title }}</span>
                                <span class="line-clamp-1 text-[7px] font-medium text-slate-600 dark:text-slate-400">{{ chip.doctorName }}</span>
                              </button>
                            }
                            @if (cell.overflowCount > 0) {
                              <span class="text-[8px] font-semibold text-slate-500 dark:text-slate-400">
                                +{{ cell.overflowCount }}
                              </span>
                            }
                          </div>
                        </div>
                      }
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        }
        </div>

        <!-- Mobile View (List) -->
        <div class="block sm:hidden mt-2">
          @if (vm.layoutMode === 'timeline' && vm.columns) {
            <div class="flex flex-col gap-4">
              @for (col of vm.columns; track col.key) {
                @if (col.appointments.length > 0) {
                  <div>
                    <h3 class="mb-2 text-sm font-bold text-slate-800 dark:text-slate-100 border-b border-slate-200 dark:border-slate-700 pb-1">
                      {{ col.header }} {{ col.sub }}
                    </h3>
                    <div class="flex flex-col gap-2">
                      @for (apt of col.appointments; track apt.id) {
                        <div
                          class="flex flex-col gap-1 rounded-lg border border-slate-200/90 bg-white p-3 shadow-sm dark:border-slate-600/70 dark:bg-[#1E293B]"
                          [ngClass]="appointmentCardStatusNgClass(apt)"
                          [style.borderLeftWidth.px]="4"
                          [style.borderLeftStyle]="'solid'"
                          [style.borderLeftColor]="resolveAppointmentHex(apt.color)"
                          (click)="onAppointmentCardClick($event, apt.id)"
                        >
                          <div class="flex items-center justify-between gap-2">
                            <div class="min-w-0">
                              <p
                                class="text-base font-bold tabular-nums text-slate-900 dark:text-slate-50"
                              >
                                {{ apt.startDisplayLabel }}
                              </p>
                              <p class="text-[10px] font-medium tabular-nums text-slate-500 dark:text-slate-400">
                                {{ apt.timeRangeLabel }}
                              </p>
                            </div>
                            <span
                              class="inline-flex shrink-0 items-center rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide text-white"
                              [style.backgroundColor]="resolveAppointmentHex(apt.typeColor)"
                            >
                              {{ apt.typeLabel }}
                            </span>
                          </div>
                          <p class="text-sm font-semibold text-slate-900 dark:text-slate-100" [class.line-through]="apt.status === 'CANCELLED'">
                            {{ apt.title }}
                          </p>
                          <div class="flex items-center gap-2 pt-1">
                            <img
                              class="size-9 shrink-0 rounded-full object-cover ring-1 ring-slate-200/90 dark:ring-slate-600"
                              [src]="doctorPortraitSrc(apt)"
                              width="36"
                              height="36"
                              alt=""
                            />
                            <p class="min-w-0 flex-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                              {{ apt.doctorName }}
                            </p>
                          </div>
                        </div>
                      }
                    </div>
                  </div>
                }
              }
              @if (!vm.timelineHasEvents) {
                <div
                  class="flex flex-col items-center rounded-xl border border-dashed border-slate-200 bg-slate-50/90 px-5 py-10 text-center dark:border-slate-600 dark:bg-slate-800/40"
                  role="status"
                >
                  <span
                    class="mb-3 flex size-12 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm ring-1 ring-slate-200 dark:bg-slate-800 dark:text-slate-500 dark:ring-slate-600"
                    aria-hidden="true"
                  >
                    <svg class="size-7" fill="none" stroke="currentColor" stroke-width="1.6" viewBox="0 0 24 24">
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5a2.25 2.25 0 002.25-2.25m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5a2.25 2.25 0 012.25 2.25v7.5"
                      />
                    </svg>
                  </span>
                  <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">Calendrier vide</p>
                  <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">Ajustez filtres ou date comme sur bureau.</p>
                </div>
              }
            </div>
          } @else if (vm.layoutMode === 'monthMatrix' && vm.monthWeekRows) {
            <div class="flex flex-col gap-4">
              @for (row of vm.monthWeekRows; track $index) {
                @for (cell of row; track cell.key) {
                  @if (cell.belongsToDisplayedMonth && cell.chips.length > 0) {
                    <div>
                      <h3 class="mb-2 text-sm font-bold text-slate-800 dark:text-slate-100 border-b border-slate-200 dark:border-slate-700 pb-1">
                        {{ cell.date.getDate() }}
                      </h3>
                      <div class="flex flex-col gap-2">
                        @for (chip of cell.chips; track chip.id) {
                          <div
                            class="flex flex-col gap-1 rounded-lg border border-slate-200/90 bg-white p-3 shadow-sm dark:border-slate-600/70 dark:bg-[#1E293B]"
                            [style.borderLeftWidth.px]="4"
                            [style.borderLeftStyle]="'solid'"
                            [style.borderLeftColor]="resolveAppointmentHex(chip.color)"
                            (click)="onAppointmentChipClick(chip.id)"
                          >
                            <div class="flex items-center justify-end">
                              <span
                                class="inline-flex items-center rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide text-white"
                                [style.backgroundColor]="resolveAppointmentHex(chip.typeColor)"
                              >
                                {{ chip.typeLabel }}
                              </span>
                            </div>
                            <p class="text-sm font-semibold text-slate-900 dark:text-slate-100">
                              {{ chip.title }}
                            </p>
                            <p class="text-xs font-medium text-slate-500 dark:text-slate-400">
                              {{ chip.doctorName }}
                            </p>
                          </div>
                        }
                      </div>
                    </div>
                  }
                }
              }
            </div>
          } @else if (vm.layoutMode === 'yearMatrix') {
            <p class="text-center text-sm text-slate-500 py-6">
              Vue année non optimisée pour liste mobile. Utilisez la vue mois.
            </p>
          }
        </div>

      </section>
    }
  `,
})
export class CalendarGridComponent {
  protected readonly VIEW_HINTS = VIEW_HINTS;
  protected readonly matrixWeekdays: readonly string[] = CALENDAR_MATRIX_WEEKDAYS;

  private readonly agenda = inject(AgendaStateService);
  private readonly theme = inject(ThemeService);
  private readonly renderer = inject(Renderer2);
  private readonly document = inject(DOCUMENT);

  resolveAppointmentHex(hex: string | undefined | null): string {
    const s = (hex ?? '').trim();
    if (!s) {
      return DEFAULT_APPOINTMENT_HEX;
    }
    const withHash = s.startsWith('#') ? s : `#${s}`;
    if (/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(withHash)) {
      return withHash;
    }
    return DEFAULT_APPOINTMENT_HEX;
  }

  monthMatrixCellNgClass(cell: MonthMatrixCellVm): Record<string, boolean> {
    const ghost = !cell.belongsToDisplayedMonth;
    return {
      'border-transparent': ghost,
      'bg-slate-100/40': ghost,
      'dark:bg-slate-800/25': ghost,
      'border-slate-200/90': !ghost,
      'bg-white': !ghost,
      'shadow-sm': !ghost,
      'dark:border-slate-600/70': !ghost,
      'dark:bg-slate-800/45': !ghost,
      'ring-2 ring-blue-500 dark:ring-blue-400': cell.isSelected,
    };
  }

  monthMatrixDayNumberNgClass(cell: MonthMatrixCellVm): Record<string, boolean> {
    const ghost = !cell.belongsToDisplayedMonth;
    const today = cell.isToday;
    return {
      'text-slate-400': ghost && !today,
      'dark:text-slate-500': ghost && !today,
      'text-slate-700': !ghost && !today,
      'dark:text-slate-200': !ghost && !today,
      'bg-blue-600': today,
      'text-white': today,
    };
  }

  yearMiniCellNgClass(cell: MonthMatrixCellVm): Record<string, boolean> {
    const ghost = !cell.belongsToDisplayedMonth;
    return {
      'bg-slate-50/80': ghost,
      'dark:bg-slate-900/40': ghost,
      'border-slate-200/80': !ghost,
      'bg-white': !ghost,
      'dark:border-slate-600/50': !ghost,
      'dark:bg-slate-800/40': !ghost,
      'ring-1 ring-blue-500': cell.isSelected,
    };
  }

  yearMiniDayNumberNgClass(cell: MonthMatrixCellVm): Record<string, boolean> {
    const ghost = !cell.belongsToDisplayedMonth;
    const today = cell.isToday;
    return {
      'text-slate-400': ghost && !today,
      'dark:text-slate-500': ghost && !today,
      'text-slate-700': !ghost && !today,
      'dark:text-slate-200': !ghost && !today,
      'text-blue-600': today,
      'dark:text-blue-400': today,
    };
  }

  cardBackgroundAlpha(): number {
    return this.theme.isDark() ? 0.2 : 0.15;
  }

  matrixChipBg(hex: string, mini = false): string {
    const base = this.cardBackgroundAlpha();
    const alpha = mini ? Math.min(0.38, base + 0.12) : base + 0.05;
    return this.hexToRgba(hex, alpha);
  }

  hexToRgba(hex: string | undefined | null, alpha: number): string {
    let h = this.resolveAppointmentHex(hex);
    if (h.length === 4) {
      const r = parseInt(h[1] + h[1], 16);
      const g = parseInt(h[2] + h[2], 16);
      const b = parseInt(h[3] + h[3], 16);
      return `rgba(${r},${g},${b},${alpha})`;
    }
    const r = parseInt(h.slice(1, 3), 16);
    const g = parseInt(h.slice(3, 5), 16);
    const b = parseInt(h.slice(5, 7), 16);
    if ([r, g, b].some((n) => Number.isNaN(n))) {
      return `rgba(59, 130, 246, ${alpha})`;
    }
    return `rgba(${r},${g},${b},${alpha})`;
  }

  readonly appointmentOpened = output<Appointment>();

  private suppressCardClickUntil = 0;

  selectMatrixDay(date: Date): void {
    this.agenda.setSelectedDate(noonDate(date));
  }

  onMatrixCellClick(ev: MouseEvent, cell: MonthMatrixCellVm): void {
    const t = ev.target as HTMLElement | null;
    if (t?.closest('[data-matrix-chip]')) {
      return;
    }
    this.selectMatrixDay(cell.date);
  }

  onAppointmentChipClick(appointmentId: string): void {
    if (Date.now() < this.suppressCardClickUntil) {
      return;
    }
    const apt = this.agenda.getAppointmentById(appointmentId);
    if (apt) {
      this.appointmentOpened.emit(apt);
    }
  }

  onAppointmentCardClick(ev: MouseEvent, appointmentId: string): void {
    ev.stopPropagation();
    if (Date.now() < this.suppressCardClickUntil) {
      return;
    }
    const apt = this.agenda.getAppointmentById(appointmentId);
    if (apt) {
      this.appointmentOpened.emit(apt);
    }
  }

  openAppointmentKeyboard(appointmentId: string, ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (Date.now() < this.suppressCardClickUntil) {
      return;
    }
    const apt = this.agenda.getAppointmentById(appointmentId);
    if (apt) {
      this.appointmentOpened.emit(apt);
    }
  }

  onAppointmentDragEnded(event: CdkDragEnd, apt: PlacedAppointment): void {
    this.draggingAptId = null;
    delete this.draggingTimes[apt.id];
    const dragEl = event.source;
    const dragRect = dragEl.element.nativeElement.getBoundingClientRect();
    const resolved = this.resolveDrop(event.dropPoint.x, event.dropPoint.y, dragRect.top);
    if (!resolved) {
      dragEl.reset();
      return;
    }

    let offsetMin = resolved.ratio * TOTAL_MINUTES;
    offsetMin = Math.max(0, Math.min(TOTAL_MINUTES, offsetMin));
    offsetMin = Math.round(offsetMin / 15) * 15;

    const maxStartOffset = Math.max(0, TOTAL_MINUTES - apt.durationMinutes);
    offsetMin = Math.min(offsetMin, maxStartOffset);

    const dayMidnight = dateFromColumnKey(resolved.key);
    const newStart = new Date(dayMidnight);
    newStart.setHours(ANCHOR_HOUR, 0, 0, 0);
    newStart.setMinutes(newStart.getMinutes() + offsetMin);

    const prevMs = apt.startTimeMs;
    if (newStart.getTime() === prevMs) {
      dragEl.reset();
      return;
    }

    const label = this.formatConfirmLabel(resolved.key, offsetMin);
    const ok = window.confirm(`Confirmez-vous le déplacement de ce rendez-vous à ${label} ?`);
    if (!ok) {
      dragEl.reset();
      return;
    }

    this.agenda.rescheduleAppointment(apt.id, newStart);
    this.suppressCardClickUntil = Date.now() + 420;
    /** Sans reset(), CDK garde le transform du drag : la carte reste décalée jusqu’au prochain refresh. */
    dragEl.reset();
  }

  private formatConfirmLabel(columnKey: string, offsetMinutesFromAnchor: number): string {
    const dayMidnight = dateFromColumnKey(columnKey);
    const start = new Date(dayMidnight);
    start.setHours(ANCHOR_HOUR, 0, 0, 0);
    start.setMinutes(start.getMinutes() + offsetMinutesFromAnchor);
    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    }).format(start);
  }

  private resolveDrop(
    pointerX: number,
    pointerY: number,
    topEdgeY: number,
  ): { key: string; ratio: number } | null {
    for (const node of document.elementsFromPoint(pointerX, pointerY)) {
      if (!(node instanceof HTMLElement)) {
        continue;
      }
      const zone = node.closest('.calendar-drop-zone');
      if (zone instanceof HTMLElement) {
        const key = zone.getAttribute('data-drop-col');
        if (!key) {
          continue;
        }
        const rect = zone.getBoundingClientRect();
        const ratio = (topEdgeY - rect.top) / rect.height;
        return { key, ratio: Math.max(0, Math.min(1, ratio)) };
      }
    }
    return null;
  }

  readonly vm$ = combineLatest({
    appointments: this.agenda.filteredAppointments$,
    view: this.agenda.currentView$,
    selectedDate: this.agenda.selectedDate$,
    doctors: this.agenda.doctors$,
    gridComfort: this.agenda.gridComfort$,
    dayShowsThreeDays: this.agenda.dayShowsThreeDays$,
  }).pipe(
    map(({ appointments, view, selectedDate, doctors, gridComfort, dayShowsThreeDays }) =>
      this.buildVm(appointments, view, selectedDate, doctors, gridComfort, dayShowsThreeDays),
    ),
  );

  private buildVm(
    appointments: Appointment[],
    view: AgendaView,
    selectedDate: Date,
    doctors: Doctor[],
    gridComfort: GridComfortMode,
    dayShowsThreeDays: boolean,
  ): CalendarVm {
    const y = selectedDate.getFullYear();
    const m0 = selectedDate.getMonth();

    if (view === 'month') {
      const doctorsById = new Map(doctors.map((d) => [d.id, d]));
      const monthWeekRows = weekRowsForCalendarMonth(
        y,
        m0,
        appointments,
        selectedDate,
        MAX_CHIPS_MONTH_VIEW,
        doctorsById,
      );
      const inMonthCount = appointments.filter(
        (a) => a.startTime.getFullYear() === y && a.startTime.getMonth() === m0,
      ).length;
      return {
        layoutMode: 'monthMatrix',
        view,
        title: new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(selectedDate),
        viewMeta: `${inMonthCount} rendez-vous ce mois (après filtres)`,
        calendarSubtitle: VIEW_HINTS.month,
        monthWeekRows,
      };
    }

    if (view === 'year') {
      const doctorsById = new Map(doctors.map((d) => [d.id, d]));
      const langFr = new Intl.DateTimeFormat('fr-FR', { month: 'long' });
      const yearMiniMonths: YearMiniMonthVm[] = [];
      for (let mi = 0; mi < 12; mi++) {
        yearMiniMonths.push({
          monthIndex: mi,
          label: langFr.format(new Date(y, mi, 1)),
          weekRows: weekRowsForCalendarMonth(
            y,
            mi,
            appointments,
            selectedDate,
            MAX_CHIPS_YEAR_MINI_DAY,
            doctorsById,
          ),
        });
      }
      return {
        layoutMode: 'yearMatrix',
        view,
        title: String(y),
        viewMeta: `${appointments.length} rendez-vous sur cette année (après filtres)`,
        calendarSubtitle: VIEW_HINTS.year,
        yearMiniMonths,
      };
    }

    const bodyHeightPx = gridBodyHeightPx(gridComfort);
    const doctorsById = new Map(doctors.map((d) => [d.id, d]));
    const columnDays = enumerateColumnDays(selectedDate, view, dayShowsThreeDays);
    const quarterTicks = buildQuarterAxisTicks();

    const dateFmt = new Intl.DateTimeFormat('fr-FR', { weekday: 'short' });
    const headerFmt = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });

    const columns: ColumnVm[] = columnDays.map((d) => ({
      key: `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`,
      sub: dateFmt.format(d).replace('.', ''),
      header: headerFmt.format(d).replace('.', ''),
      appointments: placeAppointmentsForColumn(d, appointments, doctorsById, bodyHeightPx),
    }));

    const n = columnDays.length;
    let minCol: string;
    if (n === 1) {
      minCol = 'minmax(min(300px, min(96vw, 36rem)), 1fr)';
    } else if (n <= 3) {
      minCol = 'minmax(min(200px, 30vw), 1fr)';
    } else if (n <= 7) {
      minCol = 'minmax(min(170px, 12.5vw), 1fr)';
    } else {
      minCol = 'minmax(130px, 1fr)';
    }
    const gridTemplateColumns = columnDays.map(() => minCol).join(' ');

    let title: string;
    if (view === 'week' && columnDays.length > 0) {
      const first = columnDays[0];
      const last = columnDays[columnDays.length - 1];
      const line = new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      });
      title = `Semaine : ${line.format(first)} – ${line.format(last)}`;
    } else if (view === 'day' && dayShowsThreeDays && columnDays.length > 1) {
      const first = columnDays[0];
      const last = columnDays[columnDays.length - 1];
      const short = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });
      const withYear = new Intl.DateTimeFormat('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      });
      title = `${short.format(first)} – ${withYear.format(last)}`;
    } else {
      title = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full' }).format(selectedDate);
    }
    const timelineHasEvents = columns.some((c) => c.appointments.length > 0);
    const colsLabel =
      view === 'day' ? (dayShowsThreeDays ? `3 jours` : `1 jour`) : `${columnDays.length} jour(s)`;
    const viewMeta = `${colsLabel} · repères 15 min · 08:00–19:00`;
    const calendarSubtitle =
      view === 'day'
        ? dayShowsThreeDays
          ? 'Trois colonnes à partir du jour choisi — lignes et heures tous les quart d’heure.'
          : 'Une colonne pour le jour sélectionné — lignes et heures tous les quart d’heure.'
        : 'Semaine complète — lignes et heures tous les quart d’heure pour placer les RDV avec précision.';

    return {
      layoutMode: 'timeline',
      view,
      columnDays,
      columns,
      quarterTicks,
      gridTemplateColumns,
      title,
      timelineHasEvents,
      dayShowsThreeDays: view === 'day' ? dayShowsThreeDays : false,
      bodyHeightPx,
      gridComfort,
      viewMeta,
      calendarSubtitle,
    };
  }

  setDayShowsThreeDays(value: boolean): void {
    this.agenda.setDayShowsThreeDays(value);
  }

  timelineTickLineNgClass(tick: TimeAxisTickVm): Record<string, boolean> {
    return tick.tier === 'hour'
      ? { 'border-t border-slate-200 dark:border-slate-600': true }
      : { 'border-t border-dotted border-slate-200/80 dark:border-slate-600/55': true };
  }

  timelineTickColumnLineNgClass(tick: TimeAxisTickVm): Record<string, boolean> {
    return tick.tier === 'hour'
      ? { 'border-t border-slate-100 dark:border-slate-700/45': true }
      : { 'border-t border-dotted border-slate-100/90 dark:border-slate-700/35': true };
  }

  timelineTickLabelNgClass(tick: TimeAxisTickVm): Record<string, boolean> {
    return tick.tier === 'hour'
      ? { 'text-[14px] font-bold tabular-nums text-slate-700 dark:text-slate-200': true }
      : { 'text-[11px] font-semibold tabular-nums text-slate-500 dark:text-slate-400': true };
  }

  draggingAptId: string | null = null;
  draggingTimes: Record<string, string> = {};

  resizingAptId: string | null = null;

  /** Styles RDV : annulé (barré / atténué). */
  appointmentCardStatusNgClass(apt: PlacedAppointment): Record<string, boolean> {
    return {
      'opacity-[0.72]': apt.status === 'CANCELLED',
    };
  }

  matrixChipStatusNgClass(chip: MatrixAppointmentChipVm): Record<string, boolean> {
    return {
      'opacity-80': chip.status === 'CANCELLED',
    };
  }

  /** Heure principale affichée sur la carte (timeline). */
  appointmentCardPrimaryTime(apt: PlacedAppointment): string {
    const drag = this.draggingTimes[apt.id];
    return drag !== undefined ? drag : apt.startDisplayLabel;
  }

  doctorPortraitSrc(apt: PlacedAppointment): string {
    return resolveDoctorPhotoUrl(apt.doctorPhotoUrl);
  }

  onDragStarted(apt: PlacedAppointment): void {
    this.draggingAptId = apt.id;
    this.draggingTimes[apt.id] = apt.timeRangeLabel;
  }

  onDragMoved(event: CdkDragMove, apt: PlacedAppointment, bodyHeightPx: number): void {
    if (!bodyHeightPx) return;

    const newTopPx = apt.topPx + event.distance.y;
    const deltaMinutes = (newTopPx / bodyHeightPx) * TOTAL_MINUTES;
    let startMinutesMidnight = ANCHOR_HOUR * 60 + deltaMinutes;
    startMinutesMidnight = Math.round(startMinutesMidnight / 15) * 15;
    const minStart = ANCHOR_HOUR * 60;
    const maxStart = minStart + Math.max(0, TOTAL_MINUTES - apt.durationMinutes);
    startMinutesMidnight = Math.max(minStart, Math.min(maxStart, startMinutesMidnight));

    const startStr = formatTimeFromMinutesSinceMidnight(startMinutesMidnight);
    const endStr = formatTimeFromMinutesSinceMidnight(startMinutesMidnight + apt.durationMinutes);
    this.draggingTimes[apt.id] = `${startStr} – ${endStr}`;
  }

  onResizeStart(event: MouseEvent | TouchEvent, apt: PlacedAppointment, bodyHeightPx: number): void {
    event.stopPropagation();
    event.preventDefault();

    const startY = event instanceof MouseEvent ? event.clientY : event.touches[0].clientY;
    const initialDuration = apt.durationMinutes;
    const initialHeightPx = apt.heightPx;

    const moveEvent = event instanceof MouseEvent ? 'mousemove' : 'touchmove';
    const upEvent = event instanceof MouseEvent ? 'mouseup' : 'touchend';

    this.resizingAptId = apt.id;

    const element = (event.target as HTMLElement).closest('.event-card');

    const unlistenMove = this.renderer.listen(this.document, moveEvent, (e: MouseEvent | TouchEvent) => {
      e.preventDefault();

      const currentY = e instanceof MouseEvent ? e.clientY : (e as TouchEvent).touches[0].clientY;
      const deltaY = currentY - startY;

      let deltaMinutes = (deltaY / bodyHeightPx) * TOTAL_MINUTES;
      let newDuration = initialDuration + deltaMinutes;
      newDuration = Math.round(newDuration / 15) * 15;
      if (newDuration < 15) newDuration = 15;

      if (element) {
        const heightRatio = newDuration / initialDuration;
        const newHeightPx = initialHeightPx * heightRatio;
        this.renderer.setStyle(element, 'height', `${newHeightPx}px`);
      }
    });

    const unlistenUp = this.renderer.listen(this.document, upEvent, (e: MouseEvent | TouchEvent) => {
      unlistenMove();
      unlistenUp();
      this.resizingAptId = null;

      const endY = e instanceof MouseEvent ? e.clientY : (e as TouchEvent).changedTouches[0].clientY;
      const deltaY = endY - startY;

      const deltaMinutes = (deltaY / bodyHeightPx) * TOTAL_MINUTES;
      let newDuration = initialDuration + deltaMinutes;
      newDuration = Math.round(newDuration / 15) * 15;

      if (newDuration < 15) {
        newDuration = 15;
      }

      if (newDuration !== initialDuration) {
        this.agenda.updateAppointmentDuration(apt.id, newDuration);
      } else if (element) {
        this.renderer.setStyle(element, 'height', `${initialHeightPx}px`);
      }
    });
  }
}
