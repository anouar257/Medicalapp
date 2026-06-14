import { Component, ElementRef, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
  map,
  of,
  switchMap,
} from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { PreferencesService } from '../../services/preferences.service';
import {
  PractitionerService,
  type BookingWizardOptionsDTO,
} from '../../services/practitioner.service';
import {
  AgendaService,
  type AppointmentTypeDTO,
  type DoctorReviewDTO,
  type PatientBookingRequestDTO,
} from '../../services/agenda.service';
import { ProcheService } from '../../services/proche.service';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import type { SpecialtyDTO, ConsultationLocationDTO, PractitionerProfileDTO, PractitionerActDTO } from '../../models/practitioner.model';
import type { Proche } from '../../models/proche.model';
import {
  type CombinedPractitionerOption,
  filterCombinedOptions,
  mergePractitionerSearchResults,
} from '../patient-search-merge';
import { resolveDoctorPhotoUrl, getDynamicAvatar } from '../../utils/media-url';
import * as L from 'leaflet';

@Component({
  selector: 'app-patient-booking-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppPreferencesToolbarComponent],
  templateUrl: './patient-booking-wizard.component.html',
})
export class PatientBookingWizardComponent implements OnInit, OnDestroy {
  readonly prefs = inject(PreferencesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly practitionerService = inject(PractitionerService);
  private readonly agendaService = inject(AgendaService);
  private readonly procheService = inject(ProcheService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly eRef = inject(ElementRef);

  step: 1 | 2 | 3 = 1;

  practitionerName = '';
  city = '';
  specialtyCatalog: SpecialtyDTO[] = [];
  specialtyFilterText = '';
  selectedSpecialtyLabel = '';
  isSpecialtyDropdownOpen = false;
  isCityDropdownOpen = false;
  cityFilterText = '';
  availableCities: string[] = [];
  isProcheDropdownOpen = false;
  isLocationDropdownOpen = false;

  private readonly searchSubject = new Subject<{ name: string; city: string; specialty: string }>();
  isSearching = false;
  filteredRows: CombinedPractitionerOption[] = [];
  selected: CombinedPractitionerOption | null = null;
  practitionerProfile: PractitionerProfileDTO | null = null;
  profileReviews: DoctorReviewDTO[] = [];
  loadingProfile = false;
  mapEmbedUrl: SafeResourceUrl | null = null;

  // ── Pagination ──
  currentPage = 1;
  readonly pageSize = 3;

  /** Photo resolver exposé au template. */
  resolvePhoto(url: string | undefined | null, name?: string): string {
    return resolveDoctorPhotoUrl(url, name);
  }

  onImageError(event: Event, name: string) {
    const img = event.target as HTMLImageElement;
    img.src = getDynamicAvatar(name);
  }

  beneficiary: 'self' | 'relative' = 'self';
  selectedProcheId: number | null = null;
  proches: Proche[] = [];
  loadingProches = false;

  /** Médecin agenda résolu (ligne annuaire seule → lookup par external id). */
  resolvedAgendaDoctorId: number | null = null;
  loadingStep3 = false;
  step3LoadError = '';

  wizardOptions: BookingWizardOptionsDTO | null = null;
  appointmentTypes: AppointmentTypeDTO[] = [];
  locations: ConsultationLocationDTO[] = [];
  practitionerActs: PractitionerActDTO[] = [];
  selectedAct: PractitionerActDTO | null = null;

  priorCareCode = '';
  visitReasonCode = '';
  locationMode: 'CABINET' | 'CLINIC' | 'REMOTE' = 'CABINET';
  referredBy = '';
  mapQuery = '';
  selectedLocationId: number | null = null;

  typeCode = '';
  selectedDuration = 0;
  slotLocal = '';

  submitting = false;
  submitError = '';
  submitSuccess = false;

  availableDays: { label: string; value: string }[] = [];
  minDate = '';
  maxDate = '';

  // ── Inline Calendar ──
  currentMonthDate = new Date();
  calendarWeeks: (Date | null)[][] = [];
  readonly weekDayLabels = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  readonly standardHours: string[] = [
    '08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'
  ];
  availableHours: string[] = [];
  selectedDay = '';
  selectedHour = '09:00';
  cabinetHoursForDay: string[] = [];

  // ── Hour Classification & Expansion ──
  expandedPeriods: Record<'MATIN' | 'APRESMIDI', boolean> = {
    MATIN: false,
    APRESMIDI: false,
  };

  getSlotsForPeriod(period: 'MATIN' | 'APRESMIDI'): string[] {
    const allHours = Array.from(new Set([...this.cabinetHoursForDay])).sort();
    return allHours.filter(h => {
      const hour = parseInt(h.split(':')[0], 10);
      if (period === 'MATIN') return hour < 12;
      return hour >= 12;
    });
  }

  get morningHours(): string[] { return this.getSlotsForPeriod('MATIN'); }
  get afternoonHours(): string[] { return this.getSlotsForPeriod('APRESMIDI'); }

  getDisplayedSlots(period: 'MATIN' | 'APRESMIDI'): string[] {
    const slots = this.getSlotsForPeriod(period);
    if (this.expandedPeriods[period]) {
      return slots;
    }
    return slots.slice(0, 3);
  }

  hasHiddenSlots(period: 'MATIN' | 'APRESMIDI'): boolean {
    return this.getSlotsForPeriod(period).length > 3;
  }

  togglePeriod(period: 'MATIN' | 'APRESMIDI'): void {
    this.expandedPeriods[period] = !this.expandedPeriods[period];
  }

  getCountForPeriod(period: 'MATIN' | 'APRESMIDI'): number {
    return this.availableHours.filter(h => {
      const hour = parseInt(h.split(':')[0], 10);
      if (period === 'MATIN') return hour < 12;
      return hour >= 12;
    }).length;
  }

  /** Carte Leaflet (étape 3) — géocodage via Nominatim (OpenStreetMap). */
  private map: L.Map | null = null;
  private mapMarkerLayer: L.LayerGroup | null = null;
  private mapGeocodeTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    if (!this.auth.getCurrentPatient()) {
      this.router.navigate(['/auth/login'], {
        queryParams: { returnUrl: '/patient/prendre-rendez-vous' },
      });
      return;
    }

    this.practitionerService.listSpecialties().subscribe({
      next: (list) => (this.specialtyCatalog = list ?? []),
      error: () => (this.specialtyCatalog = []),
    });

    this.practitionerService.listPublicCities().subscribe({
      next: (list) => (this.availableCities = (list ?? []).filter(Boolean)),
      error: () => (this.availableCities = []),
    });

    const qpm = this.route.snapshot.queryParamMap;
    const pid = qpm.get('practitionerId');
    const aid = qpm.get('agendaDoctorId');
    if (pid || aid) {
      this.prefillFromQuery(Number(pid || 0) || null, Number(aid || 0) || null);
    }

    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(
          (a, b) => a.name === b.name && a.city === b.city && a.specialty === b.specialty,
        ),
        switchMap((q) => {
          setTimeout(() => this.isSearching = true);
          return forkJoin({
            pros: this.practitionerService
              .searchPublic({ name: q.name, city: q.city, specialty: q.specialty })
              .pipe(catchError(() => of([]))),
            doctors: this.agendaService.listDoctors().pipe(catchError(() => of([]))),
          }).pipe(
            map(({ pros, doctors }) => {
              const merged = mergePractitionerSearchResults(pros, doctors);
              return filterCombinedOptions(merged, q.name, q.city, q.specialty);
            }),
            finalize(() => setTimeout(() => this.isSearching = false)),
          );
        }),
      )
      .subscribe((rows) => (this.filteredRows = rows));

    this.triggerSearch();

    // Pré-remplir la ville depuis le profil patient (Option 1)
    const patient = this.auth.getCurrentPatient();
    if (patient?.ville) {
      this.city = patient.ville;
      this.triggerSearch();
    }
  }

  ngOnDestroy(): void {
    this.clearMapGeocodeTimer();
    this.destroyBookingMap();
  }

  effectiveSpecialtyCode(): string {
    const c = this.selected?.specialtyCode?.trim();
    return c && c.length > 0 ? c : 'DEFAULT';
  }

  private prefillFromQuery(practitionerId: number | null, agendaDoctorId: number | null): void {
    forkJoin({
      pros: this.practitionerService.searchPublic({ name: '', city: '', specialty: '' }).pipe(catchError(() => of([]))),
      doctors: this.agendaService.listDoctors().pipe(catchError(() => of([]))),
    }).subscribe(({ pros, doctors }) => {
      const merged = mergePractitionerSearchResults(pros, doctors);
      const hit = merged.find((r) => {
        if (practitionerId && r.practitionerId === practitionerId) return true;
        if (agendaDoctorId && r.agendaDoctorId === agendaDoctorId) return true;
        return false;
      });
      if (hit) {
        this.selected = hit;
        this.goStep2();
      }
    });
  }

  selectSpecialty(s: SpecialtyDTO | null): void {
    if (!s) {
      this.specialtyFilterText = '';
      this.selectedSpecialtyLabel = '';
    } else {
      this.specialtyFilterText = s.libelle;
      this.selectedSpecialtyLabel = s.libelle;
    }
    this.isSpecialtyDropdownOpen = false;
    this.triggerSearch();
  }

  get filteredCities(): string[] {
    const query = this.cityFilterText.trim().toLowerCase();
    if (!query) {
      return this.availableCities.slice(0, 12);
    }
    return this.availableCities.filter((city) => city.toLowerCase().includes(query)).slice(0, 12);
  }

  openCityDropdown(): void {
    this.cityFilterText = this.city || '';
    this.isCityDropdownOpen = true;
  }

  selectCity(city: string | null): void {
    this.city = city ?? '';
    this.cityFilterText = city ?? '';
    this.isCityDropdownOpen = false;
    this.triggerSearch();
  }

  onCityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cityFilterText = input.value;
    this.city = input.value;
    this.triggerSearch();
  }

  triggerSearch(): void {
    this.currentPage = 1;
    this.searchSubject.next({
      name: this.practitionerName,
      city: this.city,
      specialty: this.specialtyFilterText,
    });
  }

  pickRow(row: CombinedPractitionerOption): void {
    this.selected = row;
  }

  openProfile(row: CombinedPractitionerOption): void {
    this.selected = row;
    this.goStep2();
  }

  get selectedLocation(): ConsultationLocationDTO | null {
    return this.locations.find((loc) => loc.actif) ?? this.locations[0] ?? null;
  }

  isCurrentMonth(): boolean {
    const now = new Date();
    return (
      this.currentMonthDate.getFullYear() === now.getFullYear() &&
      this.currentMonthDate.getMonth() === now.getMonth()
    );
  }

  isLastAllowedMonth(): boolean {
    const now = new Date();
    const limit = new Date(now.getFullYear(), now.getMonth() + 2, 1);
    return (
      this.currentMonthDate.getFullYear() === limit.getFullYear() &&
      this.currentMonthDate.getMonth() === limit.getMonth()
    );
  }

  get selectedProfileRating(): number {
    const backendRating = this.practitionerProfile?.globalRating ?? this.selected?.globalRating;
    if (backendRating != null && backendRating > 0) {
      return backendRating;
    }
    if (this.profileReviews.length > 0) {
      const sum = this.profileReviews.reduce((total, review) => total + Number(review.rating ?? 0), 0);
      return sum / this.profileReviews.length;
    }
    return 0;
  }

  get selectedProfileReviewCount(): number {
    const backendCount = this.practitionerProfile?.reviewCount ?? this.selected?.reviewCount;
    if (backendCount != null && backendCount > 0) {
      return backendCount;
    }
    return this.profileReviews.length;
  }

  get selectedProfileFee(): number | null {
    const profileFee = this.practitionerProfile?.consultationFee ?? this.selected?.consultationFee ?? null;
    if (profileFee != null) {
      return Number(profileFee);
    }
    const locationFee = this.selectedLocation?.consultationFee ?? null;
    return locationFee != null ? Number(locationFee) : null;
  }

  get profileHeaderAddress(): string {
    const loc = this.selectedLocation;
    if (loc) {
      const city = [loc.codePostal, loc.ville].filter(Boolean).join(' ');
      return [loc.adresse, city].filter(Boolean).join(', ');
    }
    return this.selected?.adresse || this.practitionerProfile?.organizationNom || '';
  }

  profileMapLink(): string | null {
    const address = this.profileHeaderAddress.trim();
    if (!address) return null;
    return `https://www.google.com/maps?q=${encodeURIComponent(address)}&output=embed`;
  }

  profileMapOpenLink(): string | null {
    const address = this.profileHeaderAddress.trim();
    if (!address) return null;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`;
  }

  profileStars(): number[] {
    return [1, 2, 3, 4, 5];
  }

  starsFilled(rating: number | null | undefined): number {
    const value = Number(rating ?? 0);
    return Math.max(0, Math.min(5, Math.round(value)));
  }

  // ── Pagination ──

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  get paginatedRows(): CombinedPractitionerOption[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredRows.slice(start, start + this.pageSize);
  }

  get pagesArray(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    this.currentPage = Math.max(1, Math.min(page, this.totalPages));
  }

  goStep2(): void {
    if (!this.selected) return;
    this.step = 2;
    this.beneficiary = 'self';
    this.selectedProcheId = null;
    this.isProcheDropdownOpen = false;
    this.loadProches();

    this.practitionerProfile = null;
    this.profileReviews = [];
    this.locations = [];
    this.mapEmbedUrl = null;

    if (!this.selected.practitionerId) {
      this.loadingProfile = false;
      return;
    }

    this.loadingProfile = true;
    forkJoin({
      profile: this.practitionerService
        .getPublicPractitionerProfile(this.selected.practitionerId)
        .pipe(catchError(() => of(null))),
      locations: this.practitionerService
        .getPublicPractitionerLocations(this.selected.practitionerId)
        .pipe(catchError(() => of([]))),
      reviews: this.agendaService
        .listDoctorReviews(this.selected.practitionerId)
        .pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ profile, locations, reviews }) => {
        this.practitionerProfile = profile;
        this.profileReviews = reviews ?? [];
        this.locations = locations ?? [];
        this.loadingProfile = false;
        const mapLink = this.profileMapLink();
        this.mapEmbedUrl = mapLink ? this.sanitizer.bypassSecurityTrustResourceUrl(mapLink) : null;
      },
      error: () => {
        this.practitionerProfile = null;
        this.profileReviews = [];
        this.locations = [];
        this.mapEmbedUrl = null;
        this.loadingProfile = false;
      },
    });
  }

  goStep3(): void {
    if (this.beneficiary === 'relative' && !this.selectedProcheId) return;
    if (!this.selected) return;

    this.destroyBookingMap();
    this.step = 3;
    this.step3LoadError = '';
    this.wizardOptions = null;
    this.appointmentTypes = [];
    this.locations = [];
    this.practitionerActs = [];
    this.selectedAct = null;
    this.priorCareCode = '';
    this.visitReasonCode = '';
    this.submitError = '';
    this.submitSuccess = false;
    this.selectedLocationId = null; // Force selection initially

    this.loadingStep3 = true;
    this.resolveAgendaDoctorId$()
      .pipe(
        switchMap((docId) => {
          this.resolvedAgendaDoctorId = docId;
          if (docId == null) {
            return of({
              options: null as BookingWizardOptionsDTO | null,
              types: [] as AppointmentTypeDTO[],
              locations: [] as ConsultationLocationDTO[],
              acts: [] as PractitionerActDTO[],
            });
          }
          const spec = this.effectiveSpecialtyCode();
          const pid = this.selected!.practitionerId;
          return forkJoin({
            options: this.practitionerService.getBookingWizardOptions(spec).pipe(catchError(() => of(null))),
            types: this.agendaService.listAppointmentTypes().pipe(catchError(() => of([]))),
            locations:
              pid != null
                ? this.practitionerService.getPublicPractitionerLocations(pid).pipe(catchError(() => of([])))
                : of([]),
            acts:
              pid != null
                ? this.practitionerService.getPublicActs(pid).pipe(catchError(() => of([])))
                : of([]),
          });
        }),
        finalize(() => (this.loadingStep3 = false)),
      )
      .subscribe({
        next: (data) => {
          this.wizardOptions = data.options;
          this.appointmentTypes = data.types ?? [];
          this.locations = data.locations ?? [];
          this.practitionerActs = data.acts ?? [];
          if (this.resolvedAgendaDoctorId == null) {
            this.step3LoadError = this.prefs.translate('PATIENT.BOOKING.NO_AGENDA_DOCTOR');
            return;
          }
          if (!this.practitionerActs.length) {
            this.step3LoadError = this.prefs.translate('PATIENT.BOOKING.NO_CONFIGURED_ACT');
            this.typeCode = '';
            this.selectedDuration = 0;
            return;
          }
          this.generateAvailableDays();

          this.currentMonthDate = new Date();
          this.generateCalendarCells();
          this.applyDefaultPractitionerAct();
          this.applyDefaultDateTime();
          this.scheduleBookingMapInit();
        },
        error: () => {
          this.step3LoadError = this.prefs.translate('PATIENT.BOOKING.STEP3_LOAD_ERROR');
        },
      });
  }

  applyDefaultPractitionerAct(): void {
    if (this.practitionerActs && this.practitionerActs.length > 0) {
      this.selectedAct = this.practitionerActs[0];
      this.selectedDuration = this.selectedAct.durationMinutes;
      this.visitReasonCode = this.selectedAct.name;
      this.typeCode = this.resolveAppointmentTypeCodeForAct(this.selectedAct);
    } else {
      this.selectedAct = null;
      this.applyDefaultAppointmentType();
    }
  }

  selectPractitionerAct(act: PractitionerActDTO): void {
    this.selectedAct = act;
    this.selectedDuration = act.durationMinutes;
    this.visitReasonCode = act.name;
    this.typeCode = this.resolveAppointmentTypeCodeForAct(act);

    this.fetchAvailableSlots();
  }

  private resolveAppointmentTypeCodeForAct(act: PractitionerActDTO | null | undefined): string {
    if (act?.agendaTypeCode?.trim()) {
      return act.agendaTypeCode.trim().toUpperCase();
    }
    return '';
  }

  private resolveAgendaDoctorId$() {
    const s = this.selected;
    if (!s) return of(null);
    if (s.agendaDoctorId != null) return of(s.agendaDoctorId);
    if (s.practitionerId != null) {
      return this.agendaService.getDoctorByExternalPractitionerId(s.practitionerId).pipe(
        map((d) => d.id),
        catchError(() => of(null)),
      );
    }
    return of(null);
  }

  private applyDefaultAppointmentType(): void {
    this.typeCode = '';
    this.selectedDuration = 0;
  }

  private applyDefaultDateTime(): void {
    const now = new Date();
    // Par défaut: demain à 09:00 (ou lundi si demain est dimanche)
    const defaultDay = new Date(now);
    defaultDay.setDate(now.getDate() + 1);
    
    if (defaultDay.getDay() === 0) {
      defaultDay.setDate(defaultDay.getDate() + 1);
    }
    
    defaultDay.setHours(9, 0, 0, 0);

    const y = defaultDay.getFullYear();
    const m = String(defaultDay.getMonth() + 1).padStart(2, '0');
    const d = String(defaultDay.getDate()).padStart(2, '0');
    const hh = String(defaultDay.getHours()).padStart(2, '0');
    const mm = String(defaultDay.getMinutes()).padStart(2, '0');
    
    this.slotLocal = `${y}-${m}-${d}T${hh}:${mm}`;
    this.selectedDay = `${y}-${m}-${d}`;
    this.selectedHour = ''; // Reset selected hour until slots are loaded
    
    // Fetch dynamically available slots
    this.fetchAvailableSlots();
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${dd}`;
  }

  private generateAvailableDays(): void {
    this.availableDays = [];
    const now = new Date();
    
    // Set boundaries for date input (2 months limit)
    this.minDate = this.formatDate(now);
    const max = new Date(now);
    max.setMonth(now.getMonth() + 2);
    this.maxDate = this.formatDate(max);

    for (let i = 0; i < 7; i++) {
      const d = new Date(now);
      d.setDate(now.getDate() + i);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const val = `${y}-${m}-${dd}`;
      
      let label = '';
      if (i === 0) label = this.prefs.translate('PATIENT.BOOKING.TODAY');
      else if (i === 1) label = this.prefs.translate('PATIENT.BOOKING.TOMORROW');
      else {
        label = d.toLocaleDateString(this.prefs.language() === 'fr' ? 'fr-FR' : 'en-US', {
          weekday: 'short',
          day: 'numeric',
          month: 'short',
        });
      }
      this.availableDays.push({ label, value: val });
    }
  }

  onDateTimeUIChange(): void {
    if (this.selectedDay && this.selectedHour) {
      this.slotLocal = `${this.selectedDay}T${this.selectedHour}`;
    }
  }

  formatSelectedDateLabel(): string {
    if (!this.selectedDay) return '—';
    try {
      const parts = this.selectedDay.split('-');
      const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
      const dateStr = d.toLocaleDateString(this.prefs.language() === 'fr' ? 'fr-FR' : 'en-US', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      });
      const formattedDate = dateStr.charAt(0).toUpperCase() + dateStr.slice(1);
      return `${formattedDate} ${this.prefs.translate('COMMON.AT')} ${this.selectedHour || ''}`;
    } catch {
      return `${this.selectedDay} à ${this.selectedHour}`;
    }
  }

  // ── Inline Calendar Methods ──

  generateCalendarCells(): void {
    const year = this.currentMonthDate.getFullYear();
    const month = this.currentMonthDate.getMonth();
    
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    
    let firstDayIndex = firstDay.getDay() - 1;
    if (firstDayIndex === -1) firstDayIndex = 6; // Sunday becomes 6
    
    const weeks: (Date | null)[][] = [];
    let currentWeek: (Date | null)[] = [];
    
    for (let i = 0; i < firstDayIndex; i++) {
      currentWeek.push(null);
    }
    
    const totalDays = lastDay.getDate();
    for (let day = 1; day <= totalDays; day++) {
      const d = new Date(year, month, day);
      currentWeek.push(d);
      
      if (currentWeek.length === 7) {
        weeks.push(currentWeek);
        currentWeek = [];
      }
    }
    
    if (currentWeek.length > 0) {
      while (currentWeek.length < 7) {
        currentWeek.push(null);
      }
      weeks.push(currentWeek);
    }
    
    this.calendarWeeks = weeks;
  }

  prevMonth(): void {
    const now = new Date();
    if (this.currentMonthDate.getFullYear() === now.getFullYear() && this.currentMonthDate.getMonth() === now.getMonth()) {
      return;
    }
    this.currentMonthDate = new Date(this.currentMonthDate.getFullYear(), this.currentMonthDate.getMonth() - 1, 1);
    this.generateCalendarCells();
  }

  nextMonth(): void {
    const now = new Date();
    const limit = new Date(now.getFullYear(), now.getMonth() + 2, 1);
    if (this.currentMonthDate.getFullYear() === limit.getFullYear() && this.currentMonthDate.getMonth() === limit.getMonth()) {
      return;
    }
    this.currentMonthDate = new Date(this.currentMonthDate.getFullYear(), this.currentMonthDate.getMonth() + 1, 1);
    this.generateCalendarCells();
  }

  isDateDisabled(d: Date | null): boolean {
    if (!d) return true;
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const limit = new Date(today);
    limit.setMonth(today.getMonth() + 2);
    
    const checkDate = new Date(d);
    checkDate.setHours(0, 0, 0, 0);
    
    if (checkDate.getDay() === 0) return true; // Disable Sundays
    return checkDate < today || checkDate > limit;
  }

  selectCalendarDate(d: Date): void {
    if (this.isDateDisabled(d)) return;
    this.selectedDay = this.formatDate(d);
    this.fetchAvailableSlots();
  }

  isCalendarDateSelected(d: Date | null): boolean {
    if (!d || !this.selectedDay) return false;
    return this.formatDate(d) === this.selectedDay;
  }

  currentMonthYearLabel(): string {
    const label = this.currentMonthDate.toLocaleDateString(this.prefs.language() === 'fr' ? 'fr-FR' : 'en-US', {
      month: 'long',
      year: 'numeric',
    });
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  selectShortcutDay(offset: number): void {
    const d = new Date();
    d.setDate(d.getDate() + offset);
    
    // If it's Sunday, add 1 day
    if (d.getDay() === 0) d.setDate(d.getDate() + 1);

    this.selectedDay = this.formatDate(d);
    this.currentMonthDate = new Date(d.getFullYear(), d.getMonth(), 1);
    this.generateCalendarCells();
    this.fetchAvailableSlots();
  }

  computeCabinetHoursForDay(): void {
    if (!this.selectedDay) {
      this.cabinetHoursForDay = [];
      return;
    }
    const d = new Date(this.selectedDay);
    const dayIndex = d.getDay();
    const dayNames = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];
    const dayName = dayNames[dayIndex];

    let horaires: any[] = [];
    if (this.selectedLocationId && this.selectedLocationId !== -1) {
      const loc = this.locations.find((l) => l.id === this.selectedLocationId);
      if (loc && loc.horaires) horaires = loc.horaires;
    } else if (this.locations.length > 0 && this.locations[0].horaires) {
      horaires = this.locations[0].horaires;
    }

    const dayHoraires = horaires.filter((h) => h.jour === dayName);

    if (dayHoraires.length === 0) {
      this.cabinetHoursForDay = [...this.standardHours];
      return;
    }

    const slots = new Set<string>();
    for (const h of dayHoraires) {
      if (!h.heureDebut || !h.heureFin) continue;
      const startParts = h.heureDebut.split(':').map(Number);
      const endParts = h.heureFin.split(':').map(Number);
      let currentMin = startParts[0] * 60 + startParts[1];
      const endMin = endParts[0] * 60 + endParts[1];

      while (currentMin < endMin) {
        const hh = Math.floor(currentMin / 60);
        const mm = currentMin % 60;
        const shh = String(hh).padStart(2, '0');
        const smm = String(mm).padStart(2, '0');
        slots.add(`${shh}:${smm}`);
        currentMin += this.selectedDuration || 15;
      }
    }

    this.cabinetHoursForDay = Array.from(slots).sort();

    // Add availableHours that might fall outside strictly computed slots
    for (const h of this.availableHours) {
      if (!this.cabinetHoursForDay.includes(h)) {
        this.cabinetHoursForDay.push(h);
      }
    }
    this.cabinetHoursForDay.sort();
  }

  fetchAvailableSlots(): void {
    if (!this.selectedDay || !this.resolvedAgendaDoctorId) return;
    if (this.selectedLocationId === null) return; // Must select location first
    
    this.availableHours = []; // Reset while loading
    this.cabinetHoursForDay = [];
    this.selectedHour = '';
    
    this.agendaService.getAvailableSlots(this.resolvedAgendaDoctorId, this.selectedDay, this.selectedDuration)
      .subscribe({
        next: (response: any) => {
          const rawAvailableSlots = Array.isArray(response?.availableSlots) ? response.availableSlots : [];

          // Filter out slots that have already passed locally
          const now = new Date();
          let slots = rawAvailableSlots.filter((h: string) => {
            const slotTime = new Date(`${this.selectedDay}T${h}`);
            return slotTime > now;
          });

          // Filter by selected location's schedules
          if (this.selectedLocationId && this.selectedLocationId !== -1) {
            const loc = this.locations.find(l => l.id === this.selectedLocationId);
            if (loc && loc.horaires) {
              const d = new Date(this.selectedDay);
              const dayIndex = d.getDay();
              const dayNames = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];
              const dayName = dayNames[dayIndex];
              
              const activeHoraires = loc.horaires.filter(h => h.jour === dayName);
              
              slots = slots.filter((slot: string) => {
                const [sh, sm] = slot.split(':').map(Number);
                const slotMinutes = sh * 60 + sm;
                
                return activeHoraires.some(h => {
                  const [bh, bm] = h.heureDebut.split(':').map(Number);
                  const [eh, em] = h.heureFin.split(':').map(Number);
                  const startMinutes = bh * 60 + bm;
                  const endMinutes = eh * 60 + em;
                  return slotMinutes >= startMinutes && slotMinutes < endMinutes;
                });
              });
            }
          }
          this.availableHours = slots;

          this.computeCabinetHoursForDay();

          if (this.availableHours.length > 0) {
            this.selectedHour = this.availableHours[0];
            this.onDateTimeUIChange();
          }
        },
        error: () => {
          this.availableHours = [];
          this.computeCabinetHoursForDay();
        }
      });
  }

  onTypeCodeChange(): void {
    const t = this.appointmentTypes.find((x) => x.code === this.typeCode);
    if (t) {
      this.selectedDuration = t.defaultDurationMinutes;
      this.fetchAvailableSlots();
    }
  }

  onLocationPicked(): void {
    const loc = this.locations.find((l) => l.id === this.selectedLocationId);
    if (loc) {
      this.mapQuery = [loc.nomEtablissement, loc.adresse, loc.ville, loc.codePostal].filter(Boolean).join(', ');
    }
    this.scheduleMapGeocode();
  }

  onMapQueryChange(): void {
    this.scheduleMapGeocode();
  }

  openMapInOsm(): void {
    const q = (this.mapQuery || '').trim() || this.buildMapQueryFromSelectedLocation();
    if (!q) return;
    const url = `https://www.openstreetmap.org/search?query=${encodeURIComponent(q)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  private buildMapQueryFromSelectedLocation(): string {
    const loc = this.locations.find((l) => l.id === this.selectedLocationId);
    if (!loc) return '';
    return [loc.nomEtablissement, loc.adresse, loc.ville, loc.codePostal].filter(Boolean).join(', ');
  }

  submitBooking(): void {
    this.submitError = '';
    const patient = this.auth.getCurrentPatient();
    const docId = this.resolvedAgendaDoctorId;
    if (!patient?.patientId || docId == null) {
      this.submitError = this.prefs.translate('PATIENT.BOOKING.MISSING_PATIENT_OR_DOCTOR');
      return;
    }
    
    if (!this.slotLocal?.trim()) {
      this.submitError = this.prefs.translate('PATIENT.BOOKING.SLOT_REQUIRED');
      return;
    }
    const start = new Date(this.slotLocal);
    if (Number.isNaN(start.getTime())) {
      this.submitError = this.prefs.translate('PATIENT.BOOKING.INVALID_SLOT');
      return;
    }

    if (start < new Date()) {
      this.submitError = this.prefs.translate('PATIENT.BOOKING.PAST_DATE_ERROR');
      return;
    }
    if (!this.selectedAct || !this.typeCode.trim()) {
      this.submitError = this.prefs.translate('PATIENT.BOOKING.NO_DYNAMIC_ACT');
      return;
    }


    const body: PatientBookingRequestDTO = {
      doctorId: docId,
      patientId: patient.patientId,
      patientPrenom: patient.prenom,
      patientNom: patient.nom,
      typeCode: this.typeCode,
      startTime: start.toISOString(),
      durationMinutes: this.selectedDuration,
      title: `${this.prefs.translate('PATIENT.BOOKING.REQUEST_TITLE')} — ${this.selected?.nom ?? ''}`,
      color: '#0ea5e9',
      locationMode: this.locationMode,
      referredBy: this.referredBy.trim() || null,
      priorCareCode: this.priorCareCode,
      visitReasonCode: this.visitReasonCode,
      beneficiarySummary: this.buildBeneficiarySummary(),
      mapQuery: this.mapQuery.trim() || null,
    };

    this.submitting = true;
    this.agendaService.submitPatientBooking(body).subscribe({
      next: () => {
        this.submitting = false;
        this.submitSuccess = true;
        this.destroyBookingMap();
      },
      error: (e) => {
        this.submitting = false;
        const msg = e.error?.message || e.error?.error || e.message;
        this.submitError =
          typeof msg === 'string' && msg.length > 0
            ? msg
            : this.prefs.translate('PATIENT.BOOKING.SUBMIT_ERROR');
      },
    });
  }

  private buildBeneficiarySummary(): string {
    if (this.beneficiary === 'self') return 'self';
    return `relative:${this.selectedProcheId ?? ''}`;
  }

  private clearMapGeocodeTimer(): void {
    if (this.mapGeocodeTimer != null) {
      clearTimeout(this.mapGeocodeTimer);
      this.mapGeocodeTimer = null;
    }
  }

  private scheduleMapGeocode(): void {
    this.clearMapGeocodeTimer();
    this.mapGeocodeTimer = setTimeout(() => {
      this.mapGeocodeTimer = null;
      void this.geocodeMapQuery();
    }, 600);
  }

  private destroyBookingMap(): void {
    this.clearMapGeocodeTimer();
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.mapMarkerLayer = null;
    }
  }

  private initBookingMap(): void {
    if (this.step !== 3 || this.submitSuccess) return;
    if (this.resolvedAgendaDoctorId == null || this.step3LoadError) return;

    const el = document.getElementById('patient-booking-leaflet-map');
    if (!el) return;

    this.destroyBookingMap();

    this.map = L.map(el, { scrollWheelZoom: true }).setView([46.603354, 1.888334], 6);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);
    this.mapMarkerLayer = L.layerGroup().addTo(this.map);

    void this.geocodeMapQuery();
    setTimeout(() => this.map?.invalidateSize(), 250);
  }

  private async geocodeMapQuery(): Promise<void> {
    if (!this.map || !this.mapMarkerLayer) return;
    const q = (this.mapQuery || '').trim() || this.buildMapQueryFromSelectedLocation();
    if (!q) return;

    const lang = this.prefs.language();
    const acceptLang = lang === 'fr' ? 'fr' : lang === 'ar' ? 'ar' : 'en';

    try {
      const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(q)}`;
      const res = await fetch(url, {
        headers: { Accept: 'application/json', 'Accept-Language': acceptLang },
      });
      if (!res.ok) return;
      const rows = (await res.json()) as { lat: string; lon: string }[];
      if (!rows?.length) return;
      const lat = Number(rows[0].lat);
      const lon = Number(rows[0].lon);
      if (Number.isNaN(lat) || Number.isNaN(lon)) return;

      this.mapMarkerLayer.clearLayers();
      L.circleMarker([lat, lon], {
        radius: 10,
        color: '#1d4ed8',
        weight: 3,
        fillColor: '#3b82f6',
        fillOpacity: 0.95,
      }).addTo(this.mapMarkerLayer);
      this.map.setView([lat, lon], 15);
      setTimeout(() => this.map?.invalidateSize(), 50);
    } catch {
      /* réseau / CORS / quota — la carte reste utilisable à la main */
    }
  }

  private scheduleBookingMapInit(attempts = 12): void {
    setTimeout(() => {
      const el = document.getElementById('patient-booking-leaflet-map');
      if (el && this.step === 3 && !this.submitSuccess && !this.step3LoadError) {
        this.initBookingMap();
        return;
      }
      if (attempts > 0 && this.step === 3 && !this.submitSuccess) {
        this.scheduleBookingMapInit(attempts - 1);
      }
    }, 80);
  }

  back(): void {
    if (this.step === 1) {
      this.router.navigate(['/patient/dashboard']);
      return;
    }
    if (this.step === 2) {
      this.practitionerProfile = null;
      this.locations = [];
      this.mapEmbedUrl = null;
      this.router.navigate(['/patient/prendre-rendez-vous'], { replaceUrl: true });
      this.step = 1;
      return;
    }
    this.destroyBookingMap();
    this.router.navigate(['/patient/prendre-rendez-vous'], { replaceUrl: true });
    this.step = 2;
  }

  private loadProches(): void {
    this.loadingProches = true;
    this.procheService.getMyProches().subscribe({
      next: (list) => {
        this.proches = list ?? [];
        this.loadingProches = false;
      },
      error: () => {
        this.proches = [];
        this.loadingProches = false;
      },
    });
  }

  get selectedProcheLabel(): string {
    if (this.selectedProcheId === null) {
      return this.prefs.translate('PATIENT.BOOKING.CHOOSE_RELATIVE');
    }
    const p = this.proches.find((x) => x.id === this.selectedProcheId);
    return p ? `${p.prenom} ${p.nom}` : this.prefs.translate('PATIENT.BOOKING.CHOOSE_RELATIVE');
  }

  selectProche(p: Proche | null): void {
    this.selectedProcheId = p?.id ?? null;
    this.isProcheDropdownOpen = false;
  }

  get selectedLocationLabel(): string {
    if (this.selectedLocationId === null) {
      return this.prefs.translate('PATIENT.BOOKING.CHOOSE_LOCATION');
    }
    if (this.selectedLocationId === -1) {
      return this.prefs.translate('PATIENT.BOOKING.TELECONSULTATION_ONLINE');
    }
    const loc = this.locations.find((l) => l.id === this.selectedLocationId);
    return loc ? `${loc.nomEtablissement} — ${loc.ville}` : this.prefs.translate('PATIENT.BOOKING.CHOOSE_LOCATION');
  }

  getSelectedLocation(): ConsultationLocationDTO | undefined {
    if (this.selectedLocationId === null || this.selectedLocationId === -1) {
      return undefined;
    }
    return this.locations.find((l) => l.id === this.selectedLocationId);
  }

  selectLocation(loc: ConsultationLocationDTO | null): void {
    if (loc) {
      this.selectedLocationId = loc.id ?? null;
      const name = (loc.nomEtablissement || '').toLowerCase();
      if (name.includes('clinique') || name.includes('clinic') || name.includes('hopital') || name.includes('hôpital')) {
        this.locationMode = 'CLINIC';
      } else {
        this.locationMode = 'CABINET';
      }
      this.onLocationPicked();
      this.fetchAvailableSlots();
    } else {
      this.selectedLocationId = null;
    }
  }

  selectRemoteLocation(): void {
    this.locationMode = 'REMOTE';
    this.selectedLocationId = -1;
    this.mapQuery = '';
    this.fetchAvailableSlots();
  }

  @HostListener('document:click', ['$event'])
  clickout(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.isSpecialtyDropdownOpen = false;
      this.isProcheDropdownOpen = false;
    }
  }
}
