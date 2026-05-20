import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  type PatientBookingRequestDTO,
} from '../../services/agenda.service';
import { ProcheService } from '../../services/proche.service';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import type { SpecialtyDTO, ConsultationLocationDTO } from '../../models/practitioner.model';
import type { Proche } from '../../models/proche.model';
import {
  type CombinedPractitionerOption,
  filterCombinedOptions,
  mergePractitionerSearchResults,
} from '../patient-search-merge';
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

  step: 1 | 2 | 3 = 1;

  practitionerName = '';
  city = '';
  specialtyCatalog: SpecialtyDTO[] = [];
  specialtyFilterText = '';
  selectedSpecialtyLabel = '';
  isSpecialtyDropdownOpen = false;

  private readonly searchSubject = new Subject<{ name: string; city: string; specialty: string }>();
  isSearching = false;
  filteredRows: CombinedPractitionerOption[] = [];
  selected: CombinedPractitionerOption | null = null;

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

  priorCareCode = '';
  visitReasonCode = '';
  locationMode: 'CABINET' | 'CLINIC' | 'REMOTE' = 'CABINET';
  referredBy = '';
  mapQuery = '';
  selectedLocationId: number | null = null;

  typeCode = 'CONSULTATION';
  selectedDuration = 15;
  slotLocal = '';

  submitting = false;
  submitError = '';
  submitSuccess = false;

  availableDays: { label: string; value: string }[] = [];
  availableHours: string[] = [
    '08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'
  ];
  selectedDay = '';
  selectedHour = '09:00';

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
          this.isSearching = true;
          return forkJoin({
            pros: this.practitionerService
              .searchPublic({ name: '', city: '', specialty: '' })
              .pipe(catchError(() => of([]))),
            doctors: this.agendaService.listDoctors().pipe(catchError(() => of([]))),
          }).pipe(
            map(({ pros, doctors }) => {
              const merged = mergePractitionerSearchResults(pros, doctors);
              return filterCombinedOptions(merged, q.name, q.city, q.specialty);
            }),
            finalize(() => (this.isSearching = false)),
          );
        }),
      )
      .subscribe((rows) => (this.filteredRows = rows));

    this.triggerSearch();
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
        this.step = 2;
        this.loadProches();
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

  triggerSearch(): void {
    this.searchSubject.next({
      name: this.practitionerName,
      city: this.city,
      specialty: this.specialtyFilterText,
    });
  }

  pickRow(row: CombinedPractitionerOption): void {
    this.selected = row;
  }

  goStep2(): void {
    if (!this.selected) return;
    this.step = 2;
    this.loadProches();
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
    this.priorCareCode = '';
    this.visitReasonCode = '';
    this.submitError = '';
    this.submitSuccess = false;

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
          });
        }),
        finalize(() => (this.loadingStep3 = false)),
      )
      .subscribe({
        next: (data) => {
          this.wizardOptions = data.options;
          this.appointmentTypes = data.types ?? [];
          this.locations = data.locations ?? [];
          if (this.resolvedAgendaDoctorId == null) {
            this.step3LoadError = this.prefs.translate('patient.booking.noAgendaDoctor');
            return;
          }
          this.generateAvailableDays();
          this.applyDefaultAppointmentType();
          this.applyDefaultDateTime();
          this.scheduleBookingMapInit();
        },
        error: () => {
          this.step3LoadError = this.prefs.translate('patient.booking.step3LoadError');
        },
      });
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
    if (!this.appointmentTypes.length) {
      this.typeCode = 'CONSULTATION';
      this.selectedDuration = 15;
      return;
    }
    const resolved = this.wizardOptions?.resolvedSpecialtyCode;
    const audio = this.appointmentTypes.find((t) => t.code === 'AUDIO_SESSION');
    if (resolved === 'AUDIOPROTHESE' && audio) {
      this.typeCode = 'AUDIO_SESSION';
      this.selectedDuration = audio.defaultDurationMinutes;
      return;
    }
    this.typeCode = this.appointmentTypes[0].code;
    this.selectedDuration = this.appointmentTypes[0].defaultDurationMinutes;
  }

  private applyDefaultDateTime(): void {
    const now = new Date();
    // Par défaut: demain à 09:00
    const tomorrow = new Date(now);
    tomorrow.setDate(now.getDate() + 1);
    tomorrow.setHours(9, 0, 0, 0);

    // Format ISO local pour datetime-local (YYYY-MM-DDTHH:mm)
    const y = tomorrow.getFullYear();
    const m = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const d = String(tomorrow.getDate()).padStart(2, '0');
    const hh = String(tomorrow.getHours()).padStart(2, '0');
    const mm = String(tomorrow.getMinutes()).padStart(2, '0');
    
    this.slotLocal = `${y}-${m}-${d}T${hh}:${mm}`;
    this.selectedDay = `${y}-${m}-${d}`;
    this.selectedHour = `${hh}:${mm}`;
  }

  private generateAvailableDays(): void {
    this.availableDays = [];
    const now = new Date();
    for (let i = 0; i < 7; i++) {
      const d = new Date(now);
      d.setDate(now.getDate() + i);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const val = `${y}-${m}-${dd}`;
      
      let label = '';
      if (i === 0) label = this.prefs.translate('Aujourd\'hui');
      else if (i === 1) label = this.prefs.translate('Demain');
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

  onTypeCodeChange(): void {
    const t = this.appointmentTypes.find((x) => x.code === this.typeCode);
    if (t) this.selectedDuration = t.defaultDurationMinutes;
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
      this.submitError = this.prefs.translate('patient.booking.missingPatientOrDoctor');
      return;
    }
    // Questionnaire désormais optionnel (Skip possible)
    
    if (!this.slotLocal?.trim()) {
      this.submitError = this.prefs.translate('patient.booking.slotRequired');
      return;
    }
    const start = new Date(this.slotLocal);
    if (Number.isNaN(start.getTime())) {
      this.submitError = this.prefs.translate('patient.booking.invalidSlot');
      return;
    }

    if (start < new Date()) {
      this.submitError = this.prefs.translate('Impossible de choisir une date ou heure passée');
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
      title: `${this.prefs.translate('patient.booking.requestTitle')} — ${this.selected?.nom ?? ''}`,
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
            : this.prefs.translate('patient.booking.submitError');
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

  /** Attend que le gabarit étape 3 ait rendu le conteneur (#patient-booking-leaflet-map). */
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
      this.step = 1;
      return;
    }
    this.destroyBookingMap();
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
}
