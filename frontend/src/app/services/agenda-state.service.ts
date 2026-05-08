import { DestroyRef, Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  BehaviorSubject,
  Observable,
  Subject,
  catchError,
  combineLatest,
  debounceTime,
  EMPTY,
  filter,
  map,
  merge,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import type {
  AgendaView,
  Appointment,
  AppointmentStatus,
  AppointmentType,
  Doctor,
} from '../models/agenda.model';
import { environment } from '../../environments/environment';

/** Mode grille timeline : confort / compact (voir `calendar-grid`). */
export type GridComfortMode = 'comfortable' | 'compact';

/**
 * Base URL de l’API. En dev (chaine vide) : requêtes vers `/api/...` = même origine que `ng serve`,
 * proxy → `http://localhost:8081` (voir `proxy.conf.json`).
 */
export const AGENDA_API_BASE_URL = (environment.apiBaseUrl ?? '').replace(/\/$/, '');

const LS_KEY = 'agenda-ui-v1';

type PersistedAgendaUiV1 = {
  v: 1;
  selectedDoctorIds?: string[];
  /** Codes (`CONSULTATION`, `CONTROL`, …) des types visibles. */
  visibleTypeCodes?: string[];
  /** Peut encore contenir `'3days'` (ancienne persistance) — migré vers `day` + `dayShowsThreeDays`. */
  currentView?: AgendaView | '3days';
  gridComfort?: GridComfortMode;
  /** Uniquement pour la vue `day` : afficher 3 colonnes consécutives au lieu d’une. */
  dayShowsThreeDays?: boolean;
};

type AppointmentApiDto = {
  id: number;
  title: string;
  typeId: number;
  typeCode: string;
  typeLabel: string;
  typeColor: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  description: string;
  doctorId: number;
  color: string;
  status?: string;
};

type DoctorApiDto = {
  id: number;
  name: string;
  colorCode: string;
  photoUrl?: string;
  appointmentCount?: number;
  specialty?: string | null;
};

function parseAppointmentStatus(raw: string | undefined | null): AppointmentStatus {
  if (raw === 'PENDING' || raw === 'CONFIRMED' || raw === 'CANCELLED') {
    return raw;
  }
  return 'CONFIRMED';
}

type AppointmentTypeApiDto = {
  id: number;
  code: string;
  label: string;
  colorCode: string;
  defaultDurationMinutes: number;
  displayOrder: number;
  active: boolean;
};

const ALL_VIEWS: AgendaView[] = ['day', 'week', 'month', 'year'];

function readPersisted(): PersistedAgendaUiV1 | null {
  if (typeof localStorage === 'undefined') {
    return null;
  }
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as PersistedAgendaUiV1;
  } catch {
    return null;
  }
}

function writePersisted(data: PersistedAgendaUiV1): void {
  if (typeof localStorage === 'undefined') {
    return;
  }
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(data));
  } catch {
    /* quota / mode privé */
  }
}

function coerceView(raw: string | undefined, fallback: AgendaView): AgendaView {
  if (raw === '3days') {
    return 'day';
  }
  if (raw && (ALL_VIEWS as string[]).includes(raw)) {
    return raw as AgendaView;
  }
  return fallback;
}

function coerceDoctorIds(stored: string[] | undefined, allIds: string[]): string[] {
  if (!stored?.length) {
    return [...allIds];
  }
  const set = new Set(allIds);
  const filtered = stored.filter((id) => set.has(id));
  return filtered.length ? filtered : [...allIds];
}

function coerceTypeCodes(stored: string[] | undefined, allCodes: string[]): string[] {
  if (!allCodes.length) {
    return [];
  }
  if (!stored?.length) {
    return [...allCodes];
  }
  const set = new Set(allCodes);
  const filtered = stored.filter((c) => set.has(c));
  if (!filtered.length) {
    return [...allCodes];
  }
  /** Nouveaux codes ajoutés en base (ex. SURGERY) : visibles par défaut dans les filtres. */
  const merged = new Set(filtered);
  for (const c of allCodes) {
    if (!stored.includes(c)) {
      merged.add(c);
    }
  }
  return [...merged];
}

function coerceGridComfort(_raw: string | undefined, _fallback: GridComfortMode): GridComfortMode {
  return 'comfortable';
}

function addMinutes(base: Date, minutes: number): Date {
  return new Date(base.getTime() + minutes * 60_000);
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

function appointmentsOverlapRange(
  a: Appointment,
  rangeStart: Date,
  rangeEndExclusive: Date,
): boolean {
  return a.endTime > rangeStart && a.startTime < rangeEndExclusive;
}

function mapAppointmentDto(d: AppointmentApiDto): Appointment {
  return {
    id: String(d.id),
    title: d.title,
    typeId: String(d.typeId),
    typeCode: d.typeCode,
    typeLabel: d.typeLabel,
    typeColor: d.typeColor,
    startTime: new Date(d.startTime),
    endTime: new Date(d.endTime),
    durationMinutes: d.durationMinutes,
    description: d.description ?? '',
    doctorId: String(d.doctorId),
    color: d.color,
    status: parseAppointmentStatus(d.status),
  };
}

/** Corps JSON aligné sur {@link AppointmentDTO} côté Spring (sans id pour POST). */
function toAppointmentPayload(apt: Omit<Appointment, 'id'>): Record<string, unknown> {
  const typeIdNum = parseInt(String(apt.typeId), 10);
  const doctorIdNum = parseInt(String(apt.doctorId), 10);
  const duration = Math.max(1, Math.round(Number(apt.durationMinutes)));
  const payload: Record<string, unknown> = {
    title: apt.title,
    // typeCode : secours si typeId ambigu (IDs très grands / précision JS).
    typeCode: apt.typeCode,
    startTime: apt.startTime.toISOString(),
    endTime: apt.endTime.toISOString(),
    durationMinutes: duration,
    description: apt.description ?? '',
    color: apt.color,
  };
  if (Number.isFinite(typeIdNum) && typeIdNum > 0) {
    payload['typeId'] = typeIdNum;
  }
  if (Number.isFinite(doctorIdNum) && doctorIdNum > 0) {
    payload['doctorId'] = doctorIdNum;
  }
  if (apt.status) {
    payload['status'] = apt.status;
  }
  return payload;
}

function looksLikeHtml(text: string): boolean {
  const t = text.trim().slice(0, 120).toLowerCase();
  return t.startsWith('<!doctype') || t.startsWith('<html') || t.startsWith('<');
}

/** Texte utile renvoyé par Spring (400/404/409…) pour l’alerte utilisateur. */
export function extractHttpErrorDetail(err: unknown): string | null {
  if (!err || typeof err !== 'object') {
    return null;
  }
  const e = err as { error?: unknown; message?: string };
  const body = e.error;
  if (typeof body === 'string' && body.trim()) {
    if (looksLikeHtml(body)) {
      return null;
    }
    return body.trim();
  }
  if (body && typeof body === 'object') {
    const o = body as Record<string, unknown>;
    for (const key of ['message', 'detail', 'title'] as const) {
      const v = o[key];
      if (typeof v === 'string' && v.trim()) {
        return v.trim();
      }
    }
  }
  const msg = e.message;
  if (typeof msg === 'string' && msg.trim() && !msg.startsWith('Http failure')) {
    return msg.trim();
  }
  return null;
}

@Injectable({
  providedIn: 'root',
})
export class AgendaStateService {
  private readonly persistedInit = readPersisted();

  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  private readonly selectedDateSubject = new BehaviorSubject<Date>(
    (() => {
      const t = new Date();
      t.setHours(12, 0, 0, 0);
      return t;
    })(),
  );
  private readonly migratedFromLegacyThreeDayView = this.persistedInit?.currentView === '3days';
  private readonly currentViewSubject = new BehaviorSubject<AgendaView>(
    coerceView(this.persistedInit?.currentView, 'week'),
  );
  /** Avec la vue `day` : afficher 3 colonnes (lun.–mer. autour de la date, etc.). */
  private readonly dayShowsThreeDaysSubject = new BehaviorSubject<boolean>(
    Boolean(this.migratedFromLegacyThreeDayView || this.persistedInit?.dayShowsThreeDays),
  );
  private readonly selectedDoctorsSubject = new BehaviorSubject<string[]>([]);
  /** Codes des types visibles — initialisés une fois la liste dynamique reçue. */
  private readonly visibleTypeCodesSubject = new BehaviorSubject<string[]>([]);
  private readonly gridComfortSubject = new BehaviorSubject<GridComfortMode>(
    coerceGridComfort(this.persistedInit?.gridComfort, 'comfortable'),
  );
  private readonly mobileSidebarOpenSubject = new BehaviorSubject<boolean>(false);

  private readonly doctorsSubject = new BehaviorSubject<Doctor[]>([]);
  private readonly appointmentsSubject = new BehaviorSubject<Appointment[]>([]);
  private readonly appointmentTypesSubject = new BehaviorSubject<AppointmentType[]>([]);

  /** Redéclenche un GET sur la fenêtre courante (après CRUD). */
  private readonly reload$ = new Subject<void>();

  readonly doctors$: Observable<Doctor[]> = this.doctorsSubject.asObservable().pipe(
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly appointmentTypes$: Observable<AppointmentType[]> = this.appointmentTypesSubject
    .asObservable()
    .pipe(shareReplay({ bufferSize: 1, refCount: true }));

  readonly allAppointments$: Observable<Appointment[]> = this.appointmentsSubject.asObservable().pipe(
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly filteredAppointments$: Observable<Appointment[]> = combineLatest([
    this.selectedDateSubject.asObservable(),
    this.currentViewSubject.asObservable(),
    this.dayShowsThreeDaysSubject.asObservable(),
    this.selectedDoctorsSubject.asObservable(),
    this.visibleTypeCodesSubject.asObservable(),
    this.allAppointments$,
  ]).pipe(
    map(([selectedDate, view, dayThree, doctorIds, typeCodes, all]) => {
      const doctorSet = new Set(doctorIds);
      const typeSet = new Set(typeCodes);
      const { start, endExclusive } = getViewRange(selectedDate, view, {
        dayShowsThreeDays: dayThree,
      });

      const byDoctor =
        doctorSet.size === 0 ? (): boolean => false : (a: Appointment): boolean =>
          doctorSet.has(a.doctorId);
      const byType =
        typeSet.size === 0 ? (): boolean => false : (a: Appointment): boolean => typeSet.has(a.typeCode);

      return all
        .filter((a) => byDoctor(a) && byType(a) && appointmentsOverlapRange(a, start, endExclusive))
        .slice()
        .sort((a, b) => a.startTime.getTime() - b.startTime.getTime());
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    this.bootstrapDoctorsAndAppointments();
  }

  private handleHttpError(err: unknown): void {
    console.error('[AgendaStateService]', err);
    const status =
      err && typeof err === 'object' && 'status' in err
        ? (err as { status?: number }).status
        : undefined;
    const serverDetail = extractHttpErrorDetail(err);
    if (status === 0 || status === undefined) {
      const where =
        AGENDA_API_BASE_URL
          ? ` sur ${AGENDA_API_BASE_URL}`
          : ' (cible du proxy : http://localhost:8081 — lancez le backend sur ce port, ou adaptez proxy.conf.json).';
      window.alert(
        `Impossible de joindre le serveur agenda (connexion refusée, timeout ou réseau). Démarrez le microservice Spring Boot${where} puis relancez la page (F5).`,
      );
      return;
    }
    if (status === 200) {
      window.alert(
        "La réponse n'est pas du JSON (souvent une page HTML à la place de l'API). " +
          "Vérifiez que le backend tourne sur le port 8081 et que `environment.apiBaseUrl` pointe vers " +
          "http://localhost:8081, ou utilisez `ng serve` avec proxy.conf.json vers la même origine /api.",
      );
      return;
    }
    if (serverDetail) {
      window.alert(`Erreur (${status}) : ${serverDetail}`);
      return;
    }
    window.alert(`Erreur HTTP ${status}. Consultez la console (F12) pour le détail.`);
  }

  /** Charge les médecins, les types puis les préférences persistées, puis abonne le flux des rendez-vous. */
  private bootstrapDoctorsAndAppointments(): void {
    this.fetchAppointmentTypes('initial')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();

    this.fetchDoctorsAndApplySelection('initial')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();

    merge(
      combineLatest([
        this.selectedDateSubject,
        this.currentViewSubject,
        this.dayShowsThreeDaysSubject,
        this.doctorsSubject,
      ]).pipe(
        filter(([, , , docs]) => docs.length > 0),
        map(() => undefined),
      ),
      this.reload$,
    )
      .pipe(
        debounceTime(0),
        switchMap(() => {
          const { start, endExclusive } = getViewRange(
            this.selectedDateSubject.value,
            this.currentViewSubject.value,
            { dayShowsThreeDays: this.dayShowsThreeDaysSubject.value },
          );
          return this.loadAppointments(start, endExclusive);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((apps) => this.appointmentsSubject.next(apps));
  }

  loadAppointments(startDate: Date, endDate: Date): Observable<Appointment[]> {
    const params = new HttpParams()
      .set('start', startDate.toISOString())
      .set('end', endDate.toISOString());
    return this.http
      .get<AppointmentApiDto[]>(`${AGENDA_API_BASE_URL}/api/appointments`, { params })
      .pipe(
        map((rows) => rows.map(mapAppointmentDto)),
        catchError((err) => {
          this.handleHttpError(err);
          return of([]);
        }),
      );
  }

  private refreshAppointments(): void {
    this.reload$.next();
  }

  private setDoctorsFromApiResponse(docs: Doctor[], persistSource: 'initial' | 'refresh'): void {
    this.doctorsSubject.next(docs);
    const allIds = docs.map((d) => d.id);
    const nextSel =
      persistSource === 'initial'
        ? coerceDoctorIds(this.persistedInit?.selectedDoctorIds, allIds)
        : coerceDoctorIds(this.selectedDoctorsSubject.value, allIds);
    this.selectedDoctorsSubject.next(nextSel);
    this.persistUi();
  }

  private setAppointmentTypesFromApiResponse(
    types: AppointmentType[],
    persistSource: 'initial' | 'refresh',
  ): void {
    this.appointmentTypesSubject.next(types);
    const allCodes = types.filter((t) => t.active).map((t) => t.code);
    const nextSel =
      persistSource === 'initial'
        ? coerceTypeCodes(this.persistedInit?.visibleTypeCodes, allCodes)
        : coerceTypeCodes(this.visibleTypeCodesSubject.value, allCodes);
    this.visibleTypeCodesSubject.next(nextSel);
    this.persistUi();
  }

  fetchAppointmentTypes(persistSource: 'initial' | 'refresh'): Observable<AppointmentType[]> {
    return this.http.get<AppointmentTypeApiDto[]>(`${AGENDA_API_BASE_URL}/api/appointment-types`).pipe(
      map((list) =>
        list.map((t) => ({
          id: String(t.id),
          code: t.code,
          label: t.label,
          colorCode: t.colorCode,
          defaultDurationMinutes: t.defaultDurationMinutes,
          displayOrder: t.displayOrder,
          active: t.active,
        })),
      ),
      tap((types) => this.setAppointmentTypesFromApiResponse(types, persistSource)),
      catchError((err) => {
        this.handleHttpError(err);
        return of<AppointmentType[]>([]);
      }),
    );
  }

  fetchDoctorsAndApplySelection(persistSource: 'initial' | 'refresh'): Observable<Doctor[]> {
    return this.http.get<DoctorApiDto[]>(`${AGENDA_API_BASE_URL}/api/doctors`).pipe(
      map((list) =>
        list.map((d) => ({
          id: String(d.id),
          name: d.name,
          colorCode: d.colorCode,
          photoUrl: d.photoUrl ?? '',
          appointmentCount: d.appointmentCount ?? 0,
          specialty: d.specialty?.trim() || undefined,
        })),
      ),
      tap((docs) => this.setDoctorsFromApiResponse(docs, persistSource)),
      catchError((err) => {
        this.handleHttpError(err);
        return of<Doctor[]>([]);
      }),
    );
  }

  /** POST `/api/doctors` puis resynchronisation de la liste. */
  createDoctor(input: { name: string; colorCode?: string; photoUrl?: string; specialty?: string }): Observable<Doctor> {
    return this.http
      .post<DoctorApiDto>(`${AGENDA_API_BASE_URL}/api/doctors`, {
        name: input.name.trim(),
        colorCode: input.colorCode?.trim() || undefined,
        photoUrl: input.photoUrl?.trim() || undefined,
        specialty: input.specialty?.trim() || undefined,
      })
      .pipe(
        switchMap((d) =>
          this.fetchDoctorsAndApplySelection('refresh').pipe(
            map(() => ({
              id: String(d.id),
              name: d.name,
              colorCode: d.colorCode,
              photoUrl: d.photoUrl ?? '',
              specialty: d.specialty?.trim() || undefined,
            })),
          ),
        ),
        catchError((err) => throwError(() => err)),
      );
  }

  /** Upload d’image (JPEG, PNG, WebP, GIF) — enregistrement disque + URL en base. */
  uploadDoctorPhoto(id: string, file: File): Observable<Doctor> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http
      .post<DoctorApiDto>(
        `${AGENDA_API_BASE_URL}/api/doctors/${encodeURIComponent(id)}/photo`,
        formData,
      )
      .pipe(
        switchMap((d) =>
          this.fetchDoctorsAndApplySelection('refresh').pipe(
            map(() => ({
              id: String(d.id),
              name: d.name,
              colorCode: d.colorCode,
              photoUrl: d.photoUrl ?? '',
              appointmentCount: d.appointmentCount ?? 0,
              specialty: d.specialty?.trim() || undefined,
            })),
          ),
        ),
        catchError((err) => throwError(() => err)),
      );
  }

  updateDoctor(
    id: string,
    input: { name: string; colorCode?: string; photoUrl?: string; specialty?: string },
  ): Observable<void> {
    const body: {
      name: string;
      colorCode?: string;
      photoUrl?: string;
      specialty?: string;
    } = {
      name: input.name.trim(),
    };
    const color = input.colorCode?.trim();
    if (color) {
      body.colorCode = color;
    }
    const photo = input.photoUrl?.trim();
    if (photo) {
      body.photoUrl = photo;
    }
    const spec = input.specialty?.trim();
    if (spec) {
      body.specialty = spec;
    }
    return this.http
      .put<DoctorApiDto>(`${AGENDA_API_BASE_URL}/api/doctors/${encodeURIComponent(id)}`, body)
      .pipe(
        switchMap(() => this.fetchDoctorsAndApplySelection('refresh')),
        map(() => undefined),
        catchError((err) => throwError(() => err)),
      );
  }

  deleteDoctor(id: string): Observable<void> {
    return this.http
      .delete<void>(`${AGENDA_API_BASE_URL}/api/doctors/${encodeURIComponent(id)}`)
      .pipe(
        switchMap(() => this.fetchDoctorsAndApplySelection('refresh')),
        map(() => undefined),
        catchError((err: unknown) => {
          const status =
            err && typeof err === 'object' && 'status' in err
              ? (err as { status?: number }).status
              : undefined;
          if (status === 409) {
            return throwError(() => err);
          }
          this.handleHttpError(err);
          return EMPTY;
        }),
      );
  }

  createAppointment(data: Omit<Appointment, 'id'>): Observable<void> {
    return this.http
      .post<AppointmentApiDto>(`${AGENDA_API_BASE_URL}/api/appointments`, toAppointmentPayload(data))
      .pipe(
        tap(() => this.refreshAppointments()),
        map(() => undefined),
        catchError((err) => {
          this.handleHttpError(err);
          return EMPTY;
        }),
      );
  }

  updateAppointment(id: string, data: Omit<Appointment, 'id'>): Observable<void> {
    return this.http
      .put<AppointmentApiDto>(
        `${AGENDA_API_BASE_URL}/api/appointments/${encodeURIComponent(id)}`,
        toAppointmentPayload(data),
      )
      .pipe(
        tap(() => this.refreshAppointments()),
        map(() => undefined),
        catchError((err) => {
          this.handleHttpError(err);
          return EMPTY;
        }),
      );
  }

  deleteAppointment(id: string): Observable<void> {
    return this.http
      .delete<void>(`${AGENDA_API_BASE_URL}/api/appointments/${encodeURIComponent(id)}`)
      .pipe(
        tap(() => this.refreshAppointments()),
        map(() => undefined),
        catchError((err) => {
          this.handleHttpError(err);
          return EMPTY;
        }),
      );
  }

  get selectedDate(): Date {
    return this.selectedDateSubject.value;
  }

  get currentView(): AgendaView {
    return this.currentViewSubject.value;
  }

  get selectedDoctorIds(): string[] {
    return this.selectedDoctorsSubject.value;
  }

  get visibleTypeCodes(): string[] {
    return this.visibleTypeCodesSubject.value;
  }

  get appointmentTypes(): AppointmentType[] {
    return this.appointmentTypesSubject.value;
  }

  readonly selectedDate$ = this.selectedDateSubject.asObservable();

  readonly currentView$ = this.currentViewSubject.asObservable();

  readonly dayShowsThreeDays$ = this.dayShowsThreeDaysSubject.asObservable();

  readonly selectedDoctors$ = this.selectedDoctorsSubject.asObservable();

  readonly visibleTypeCodes$ = this.visibleTypeCodesSubject.asObservable();

  readonly gridComfort$ = this.gridComfortSubject.asObservable();

  readonly mobileSidebarOpen$ = this.mobileSidebarOpenSubject.asObservable();

  get gridComfort(): GridComfortMode {
    return this.gridComfortSubject.value;
  }

  get dayShowsThreeDays(): boolean {
    return this.dayShowsThreeDaysSubject.value;
  }

  setDayShowsThreeDays(value: boolean): void {
    this.dayShowsThreeDaysSubject.next(value);
    this.persistUi();
  }

  setSelectedDate(date: Date): void {
    this.selectedDateSubject.next(new Date(date.getTime()));
  }

  setCurrentView(view: AgendaView): void {
    this.currentViewSubject.next(view);
    this.persistUi();
  }

  setSelectedDoctorIds(ids: string[]): void {
    this.selectedDoctorsSubject.next([...ids]);
    this.persistUi();
  }

  toggleDoctor(id: string): void {
    const next = new Set(this.selectedDoctorsSubject.value);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.selectedDoctorsSubject.next([...next]);
    this.persistUi();
  }

  setVisibleTypeCodes(codes: string[]): void {
    this.visibleTypeCodesSubject.next([...codes]);
    this.persistUi();
  }

  toggleVisibleTypeCode(code: string): void {
    const next = new Set(this.visibleTypeCodesSubject.value);
    if (next.has(code)) {
      next.delete(code);
    } else {
      next.add(code);
    }
    this.visibleTypeCodesSubject.next([...next]);
    this.persistUi();
  }

  setGridComfortMode(mode: GridComfortMode): void {
    this.gridComfortSubject.next(mode);
    this.persistUi();
  }

  toggleMobileSidebar(): void {
    this.mobileSidebarOpenSubject.next(!this.mobileSidebarOpenSubject.value);
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpenSubject.next(false);
  }

  private persistUi(): void {
    writePersisted({
      v: 1,
      selectedDoctorIds: this.selectedDoctorsSubject.value,
      visibleTypeCodes: this.visibleTypeCodesSubject.value,
      currentView: this.currentViewSubject.value,
      gridComfort: 'comfortable',
      dayShowsThreeDays: this.dayShowsThreeDaysSubject.value || undefined,
    });
  }

  getAppointmentById(id: string): Appointment | undefined {
    const a = this.appointmentsSubject.value.find((x) => x.id === id);
    if (!a) {
      return undefined;
    }
    return {
      ...a,
      startTime: new Date(a.startTime),
      endTime: new Date(a.endTime),
    };
  }

  /**
   * Déplacement drag & drop — conserve la durée, synchronise via PUT.
   * Mise à jour optimiste du store pour repositionner la carte tout de suite ; en cas d’erreur API,
   * rechargement depuis le serveur pour annuler.
   */
  rescheduleAppointment(appointmentId: string, newStartTime: Date): void {
    const prev = this.appointmentsSubject.value.find((a) => a.id === appointmentId);
    if (!prev) {
      return;
    }
    const duration = prev.durationMinutes;
    const start = new Date(newStartTime.getTime());
    const end = addMinutes(start, duration);
    const optimistic: Appointment = {
      ...prev,
      startTime: start,
      endTime: end,
      durationMinutes: duration,
    };
    this.appointmentsSubject.next(
      this.appointmentsSubject.value.map((a) => (a.id === appointmentId ? optimistic : a)),
    );

    this.http
      .put<AppointmentApiDto>(
        `${AGENDA_API_BASE_URL}/api/appointments/${encodeURIComponent(appointmentId)}`,
        toAppointmentPayload({
          title: prev.title,
          typeId: prev.typeId,
          typeCode: prev.typeCode,
          typeLabel: prev.typeLabel,
          typeColor: prev.typeColor,
          startTime: start,
          endTime: end,
          durationMinutes: duration,
          description: prev.description,
          doctorId: prev.doctorId,
          color: prev.color,
          status: prev.status,
        }),
      )
      .pipe(
        tap(() => this.refreshAppointments()),
        catchError((err) => {
          this.handleHttpError(err);
          this.refreshAppointments();
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  updateAppointmentDuration(appointmentId: string, newDurationMinutes: number): void {
    const prev = this.appointmentsSubject.value.find((a) => a.id === appointmentId);
    if (!prev) {
      return;
    }
    const start = new Date(prev.startTime.getTime());
    const end = addMinutes(start, newDurationMinutes);
    this.updateAppointment(appointmentId, {
      title: prev.title,
      typeId: prev.typeId,
      typeCode: prev.typeCode,
      typeLabel: prev.typeLabel,
      typeColor: prev.typeColor,
      startTime: start,
      endTime: end,
      durationMinutes: newDurationMinutes,
      description: prev.description,
      doctorId: prev.doctorId,
      color: prev.color,
      status: prev.status,
    }).subscribe();
  }
}
