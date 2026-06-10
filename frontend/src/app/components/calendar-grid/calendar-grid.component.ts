import { DragDropModule, CdkDragEnd, CdkDragMove } from '@angular/cdk/drag-drop';
import { AsyncPipe, NgClass, DOCUMENT } from '@angular/common';
import { Component, inject, output, Renderer2, HostListener } from '@angular/core';
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

/** 15 minutes en ms — référence pour hauteur minimale lisible et pour les lanes. */
const FIFTEEN_MIN_MS = 15 * 60 * 1000;

/**
 * Hauteur de la timeline : 280 px/h (confort) ou 140 px/h (compact).
 * Très courts RDV : hauteur d’affichage ≥ 1 créneau de 15 min (lisibilité), le haut de carte reste à l’heure de début.
 * Ex. confort : 15 min ≈ 70 px par créneau quart d’heure.
 */
function gridBodyHeightPx(mode: GridComfortMode, hoursSpan: number): number {
  const pxPerHour = mode === 'comfortable' ? 280 : 140;
  return hoursSpan * pxPerHour;
}

/**
 * Hauteur d’un créneau de 15 min sur la timeline (les RDV plus courts utilisent au moins cette hauteur
 * pour afficher badge + horaire + titre + médecin, tout en gardant le haut de carte aligné sur l’heure exacte).
 */
function oneQuarterSlotHeightPx(bodyHeightPx: number, totalMinutes: number): number {
  return (15 / totalMinutes) * bodyHeightPx;
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

function minutesFromAnchor(d: Date, anchorHour: number): number {
  return d.getHours() * 60 + d.getMinutes() + d.getSeconds() / 60 - anchorHour * 60;
}

function clampMinutesToGrid(m: number, totalMinutes: number): number {
  if (Number.isNaN(m)) return 0;
  return Math.max(0, Math.min(totalMinutes, m));
}

interface TimeAxisTickVm {
  key: string;
  topPct: number;
  label: string;
  tier: 'hour' | 'quarter';
}

function buildQuarterAxisTicks(anchorHour: number, totalMinutes: number): TimeAxisTickVm[] {
  const ticks: TimeAxisTickVm[] = [];
  for (let m = 0; m <= totalMinutes; m += 15) {
    const totalMinDay = anchorHour * 60 + m;
    const hh24 = Math.floor(totalMinDay / 60);
    const mmPart = totalMinDay % 60;
    const isHour = mmPart === 0;
    ticks.push({
      key: `q-${m}`,
      topPct: (m / totalMinutes) * 100,
      label: `${String(hh24).padStart(2, '0')}:${String(mmPart).padStart(2, '0')}`,
      tier: isHour ? 'hour' : 'quarter',
    });
  }
  return ticks;
}

interface ColumnVm {
  key: string;
  header: string;
  sub: string;
  appointments: PlacedAppointment[];
}

interface PlacedAppointment {
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
  locationMode?: string;
}

interface MatrixAppointmentChipVm {
  id: string;
  title: string;
  startLabel: string;
  doctorName: string;
  color: string;
  typeLabel: string;
  typeColor: string;
  status?: AppointmentStatus;
  locationMode?: string;
}

interface MonthMatrixCellVm {
  key: string;
  date: Date;
  belongsToDisplayedMonth: boolean;
  isSelected: boolean;
  isToday: boolean;
  chips: MatrixAppointmentChipVm[];
  overflowCount: number;
}

interface YearMiniMonthVm {
  monthIndex: number;
  label: string;
  weekRows: MonthMatrixCellVm[][];
}

type CalendarLayoutMode = 'timeline' | 'monthMatrix' | 'yearMatrix';

interface CalendarVm {
  layoutMode: CalendarLayoutMode;
  view: AgendaView;
  title: string;
  viewMeta: string;
  calendarSubtitle: string;
  totalEventsCount?: number;
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
}

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
    locationMode: a.locationMode,
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
  anchorHour: number,
  totalMinutes: number,
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
      const s = minutesFromAnchor(a.startTime, anchorHour);
      const e = minutesFromAnchor(a.endTime, anchorHour);
      const visS = clampMinutesToGrid(s, totalMinutes);
      const visE = clampMinutesToGrid(e, totalMinutes);
      if (visE <= visS) {
        return null;
      }

      const rawHeightPx = ((visE - visS) / totalMinutes) * bodyHeightPx;
      const topPx = (visS / totalMinutes) * bodyHeightPx;
      const minReadablePx = oneQuarterSlotHeightPx(bodyHeightPx, totalMinutes);
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
        locationMode: a.locationMode,
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
  templateUrl: './calendar-grid.component.html',
  styleUrls: ['./calendar-grid.component.scss'],
})
export class CalendarGridComponent {
  protected readonly VIEW_HINTS = VIEW_HINTS;
  protected readonly matrixWeekdays: readonly string[] = CALENDAR_MATRIX_WEEKDAYS;

  private readonly agenda = inject(AgendaStateService);
  protected currentAnchorHour = 8;
  protected currentTotalMinutes = 660;
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
    const h = this.resolveAppointmentHex(hex);
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
  readonly medicalRecordOpened = output<Appointment>();

  // Context Menu state
  protected contextMenuVisible = false;
  protected contextMenuX = 0;
  protected contextMenuY = 0;
  protected contextMenuAppointmentId: string | null = null;

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
      this.medicalRecordOpened.emit(apt);
    }
  }

  onAppointmentCardClick(ev: MouseEvent, appointmentId: string): void {
    ev.stopPropagation();
    if (Date.now() < this.suppressCardClickUntil) {
      return;
    }
    const apt = this.agenda.getAppointmentById(appointmentId);
    if (apt) {
      this.medicalRecordOpened.emit(apt);
    }
  }

  onAppointmentCardRightClick(event: MouseEvent, appointmentId: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.contextMenuVisible = true;
    this.contextMenuX = event.clientX;
    this.contextMenuY = event.clientY;
    this.contextMenuAppointmentId = appointmentId;
  }

  onAppointmentChipRightClick(event: MouseEvent, appointmentId: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.contextMenuVisible = true;
    this.contextMenuX = event.clientX;
    this.contextMenuY = event.clientY;
    this.contextMenuAppointmentId = appointmentId;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    this.contextMenuVisible = false;
  }

  modifyAppointmentFromMenu(appointmentId: string): void {
    this.contextMenuVisible = false;
    const apt = this.agenda.getAppointmentById(appointmentId);
    if (apt) {
      this.appointmentOpened.emit(apt);
    }
  }

  deleteAppointmentFromMenu(appointmentId: string): void {
    this.contextMenuVisible = false;
    const ok = window.confirm('Confirmez-vous la suppression de ce rendez-vous ?');
    if (ok) {
      this.agenda.deleteAppointment(appointmentId).subscribe();
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
      this.medicalRecordOpened.emit(apt);
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

    let offsetMin = resolved.ratio * this.currentTotalMinutes;
    offsetMin = Math.max(0, Math.min(this.currentTotalMinutes, offsetMin));
    offsetMin = Math.round(offsetMin / 15) * 15;

    const maxStartOffset = Math.max(0, this.currentTotalMinutes - apt.durationMinutes);
    offsetMin = Math.min(offsetMin, maxStartOffset);

    const dayMidnight = dateFromColumnKey(resolved.key);
    const newStart = new Date(dayMidnight);
    newStart.setHours(this.currentAnchorHour, 0, 0, 0);
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
    start.setHours(this.currentAnchorHour, 0, 0, 0);
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
    anchorHour: this.agenda.anchorHour$,
    endHour: this.agenda.endHour$,
  }).pipe(
    map(({ appointments, view, selectedDate, doctors, gridComfort, dayShowsThreeDays, anchorHour, endHour }) =>
      this.buildVm(appointments, view, selectedDate, doctors, gridComfort, dayShowsThreeDays, anchorHour, endHour),
    ),
  );

  private buildVm(
    appointments: Appointment[],
    view: AgendaView,
    selectedDate: Date,
    doctors: Doctor[],
    gridComfort: GridComfortMode,
    dayShowsThreeDays: boolean,
    anchorHour: number,
    endHour: number,
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
        viewMeta: `Vue mensuelle`,
        calendarSubtitle: VIEW_HINTS.month,
        totalEventsCount: inMonthCount,
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
        viewMeta: `Vue annuelle`,
        calendarSubtitle: VIEW_HINTS.year,
        totalEventsCount: appointments.length,
        yearMiniMonths,
      };
    }

    const hoursSpan = endHour - anchorHour;
    const totalMinutes = hoursSpan * 60;
    this.currentAnchorHour = anchorHour;
    this.currentTotalMinutes = totalMinutes;

    const bodyHeightPx = gridBodyHeightPx(gridComfort, hoursSpan);
    const doctorsById = new Map(doctors.map((d) => [d.id, d]));
    const columnDays = enumerateColumnDays(selectedDate, view, dayShowsThreeDays);
    const quarterTicks = buildQuarterAxisTicks(anchorHour, totalMinutes);

    const dateFmt = new Intl.DateTimeFormat('fr-FR', { weekday: 'short' });
    const headerFmt = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });

    const columns: ColumnVm[] = columnDays.map((d) => ({
      key: `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`,
      sub: dateFmt.format(d).replace('.', ''),
      header: headerFmt.format(d).replace('.', ''),
      appointments: placeAppointmentsForColumn(d, appointments, doctorsById, bodyHeightPx, anchorHour, totalMinutes),
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
    const totalEventsCount = columns.reduce((acc, c) => acc + c.appointments.length, 0);
    const colsLabel =
      view === 'day' ? (dayShowsThreeDays ? `3 jours` : `1 jour`) : `${columnDays.length} jour(s)`;
    const startStr = `${String(anchorHour).padStart(2, '0')}:00`;
    const endStr = `${String(endHour).padStart(2, '0')}:00`;
    const viewMeta = `${colsLabel} · repères 15 min · ${startStr}–${endStr}`;
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
      totalEventsCount,
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

  /** Styles RDV : annulé (barré / atténué), honoré, ou absent. */
  appointmentCardStatusNgClass(apt: PlacedAppointment): Record<string, boolean> {
    return {
      'opacity-[0.72]': apt.status === 'CANCELLED',
      'opacity-[0.78]': apt.status === 'COMPLETED',
      'opacity-[0.65] bg-slate-100 text-slate-400 border-slate-300 dark:bg-slate-800/80 dark:text-slate-500 dark:border-slate-700': apt.status === 'NO_SHOW',
    };
  }

  matrixChipStatusNgClass(chip: MatrixAppointmentChipVm): Record<string, boolean> {
    return {
      'opacity-80': chip.status === 'CANCELLED',
      'opacity-[0.78]': chip.status === 'COMPLETED',
      'opacity-[0.65] line-through bg-slate-100 dark:bg-slate-800/80 text-slate-400 dark:text-slate-500 border-slate-300 dark:border-slate-700': chip.status === 'NO_SHOW',
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
    const deltaMinutes = (newTopPx / bodyHeightPx) * this.currentTotalMinutes;
    let startMinutesMidnight = this.currentAnchorHour * 60 + deltaMinutes;
    startMinutesMidnight = Math.round(startMinutesMidnight / 15) * 15;
    const minStart = this.currentAnchorHour * 60;
    const maxStart = minStart + Math.max(0, this.currentTotalMinutes - apt.durationMinutes);
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

      const deltaMinutes = (deltaY / bodyHeightPx) * this.currentTotalMinutes;
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

      const deltaMinutes = (deltaY / bodyHeightPx) * this.currentTotalMinutes;
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
