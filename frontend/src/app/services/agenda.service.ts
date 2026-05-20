import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { environment } from '../../environments/environment';

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export interface AppointmentTypeDTO {
  id: number;
  code: string;
  label: string;
  colorCode: string;
  defaultDurationMinutes: number;
  displayOrder: number;
  active: boolean;
}

/** Corps POST `/api/appointments/patient-booking` (demande patient → statut PENDING). */
export interface PatientBookingRequestDTO {
  doctorId: number;
  patientId: number;
  typeCode: string;
  startTime: string;
  durationMinutes: number;
  title: string;
  color: string;
  locationMode: 'CABINET' | 'CLINIC' | 'REMOTE';
  referredBy?: string | null;
  priorCareCode: string;
  visitReasonCode: string;
  beneficiarySummary: string;
  mapQuery?: string | null;
  patientPrenom?: string | null;
  patientNom?: string | null;
}

/** Médecin agenda (admin / synchro practitioner) — exposé pour la prise de RDV patient. */
export interface AgendaDoctorListDTO {
  id: number;
  name: string;
  colorCode: string;
  photoUrl: string;
  appointmentCount: number;
  specialty?: string | null;
  externalPractitionerId?: number | null;
  /** Code spécialité (synchro practitioner) pour le parcours patient. */
  specialtyCode?: string | null;
}

export interface AppointmentPatientDTO {
  id: number;
  title: string;
  patientId?: number | null;

  startTime: string; // ISO-8601 from backend
  endTime: string; // ISO-8601 from backend
  durationMinutes: number;

  typeLabel: string;
  doctorId: number;
  doctorName: string;
  doctorSpecialty: string;
  /** Profil praticien (messagerie). */
  doctorExternalPractitionerId?: number | null;

  status?: AppointmentStatus;
}

/** Liste cabinet — demandes PENDING (JWT pro). */
export interface AppointmentCabinetPendingDTO {
  id: number;
  title: string;
  patientId?: number | null;
  patientPrenom?: string | null;
  patientNom?: string | null;
  visitReasonCode?: string | null;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  typeLabel: string;
  typeCode?: string | null;
  doctorId: number;
  doctorName: string;
  doctorSpecialty?: string | null;
  status?: AppointmentStatus;
  description?: string | null;
}

/** GET `/api/admin/global-stats` (administration plateforme : JWT ADMIN sans cabinet). */
export interface GlobalPlatformStatsDTO {
  totalActiveCabinets: number;
  totalPractitionerProfiles: number;
  totalPatients: number;
  totalDoctorsInAgenda: number;
  totalSyncedDoctorsInAgenda: number;
  totalAppointments: number;
}

@Injectable({ providedIn: 'root' })
export class AgendaService {
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');
  private readonly http = inject(HttpClient);

  /** Liste des médecins du cabinet (agenda-service) — utilisée pour la recherche / prise de RDV. */
  listDoctors(): Observable<AgendaDoctorListDTO[]> {
    return this.http.get<AgendaDoctorListDTO[]>(`${this.base}/api/doctors`);
  }

  getDoctorByExternalPractitionerId(externalPractitionerId: number): Observable<AgendaDoctorListDTO> {
    return this.http.get<AgendaDoctorListDTO>(
      `${this.base}/api/doctors/by-external/${externalPractitionerId}`,
    );
  }

  /** GET `/api/appointments/patient/{patientId}`. */
  getAppointmentsForPatient(patientId: number): Observable<AppointmentPatientDTO[]> {
    return this.http
      .get<AppointmentPatientDTO[]>(`${this.base}/api/appointments/patient/${patientId}`)
      .pipe(
        map((list) =>
          list.map((a) => ({
            ...a,
            doctorSpecialty: a.doctorSpecialty ?? '',
            doctorName: a.doctorName ?? '',
            status: a.status,
            doctorExternalPractitionerId: a.doctorExternalPractitionerId ?? null,
          })),
        ),
      );
  }

  listAppointmentTypes(): Observable<AppointmentTypeDTO[]> {
    return this.http.get<AppointmentTypeDTO[]>(`${this.base}/api/appointment-types`);
  }

  /** Demande de RDV patient (statut PENDING, à valider côté cabinet). */
  submitPatientBooking(body: PatientBookingRequestDTO): Observable<AppointmentPatientDTO> {
    return this.http.post<AppointmentPatientDTO>(`${this.base}/api/appointments/patient-booking`, body);
  }

  /** Demandes PENDING pour le cabinet (JWT pro requis). */
  listCabinetPending(): Observable<AppointmentCabinetPendingDTO[]> {
    return this.http.get<AppointmentCabinetPendingDTO[]>(`${this.base}/api/appointments/cabinet/pending`);
  }

  patchAppointmentStatus(
    id: number,
    status: Extract<AppointmentStatus, 'CONFIRMED' | 'CANCELLED'>,
  ): Observable<AppointmentPatientDTO> {
    return this.http.patch<AppointmentPatientDTO>(`${this.base}/api/appointments/${id}/status`, {
      status,
    });
  }

  /** Statistiques globales plateforme (JWT admin plateforme, agenda-service).
 *
 * <p>Le backend agenda agrège : compteurs agenda (BDD locale) + appel
 * {@code GET /api/pro/platform/stats/platform-summary} sur le practitioner-service
 * (même en-tête {@code Authorization}) pour cabinets actifs, profils praticiens et patients.
 */
  getGlobalPlatformStats(): Observable<GlobalPlatformStatsDTO> {
    return this.http.get<GlobalPlatformStatsDTO>(`${this.base}/api/admin/global-stats`);
  }
}

