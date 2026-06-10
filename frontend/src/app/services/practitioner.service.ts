import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ConsultationLocationDTO,
  CreateProUserRequest,
  DiplomaDTO,
  MedicalOrganizationDTO,
  PractitionerActDTO,
  PractitionerProfileDTO,
  ProUserDTO,
  SpecialtyDTO,
  VerificationStatus,
} from '../models/practitioner.model';

/**
 * Façade Angular pour les ressources métier du practitioner-service :
 * profils praticiens, lieux de consultation, diplômes, cabinet, spécialités.
 */
@Injectable({ providedIn: 'root' })
export class PractitionerService {

  private readonly base = `${environment.practitionerApiBaseUrl}/api/pro`;
  private readonly platformAdminBase = `${environment.practitionerApiBaseUrl}/api/pro/platform`;
  private readonly publicBase = `${environment.practitionerApiBaseUrl}/api/pro/public`;

  constructor(private http: HttpClient) {}

  // ── Catalogues ─────────────────────────────────────────────────────────

  listSpecialties(): Observable<SpecialtyDTO[]> {
    return this.http.get<SpecialtyDTO[]>(`${this.base}/specialties/public/list`);
  }

  // ── Recherche publique (Landing Page) ────────────────────────────────
  /**
   * Typeahead : recherche par nom, ville et/ou spécialité (filtres partiels).
   */
  searchPublic(filters: { name?: string; city?: string; specialty?: string }): Observable<PractitionerSearchResult[]> {
    const params = {
      name: filters.name ?? '',
      ville: filters.city ?? '',
      specialty: filters.specialty ?? '',
    };

    return this.http.get<PractitionerSearchResult[]>(`${this.publicBase}/practitioners/search`, {
      params,
    });
  }

  /** Liste des villes publiques disponibles pour les filtres de recherche. */
  listPublicCities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.publicBase}/cities`);
  }

  /** Top profils publics mis en avant sur la landing page. */
  listFeaturedPublicPractitioners(limit = 3): Observable<PractitionerSearchResult[]> {
    return this.http.get<PractitionerSearchResult[]>(`${this.publicBase}/practitioners/featured`, {
      params: { limit },
    });
  }

  // ── Profil praticien ───────────────────────────────────────────────────

  me(): Observable<PractitionerProfileDTO> {
    return this.http.get<PractitionerProfileDTO>(`${this.base}/practitioners/me`);
  }

  getById(id: number): Observable<PractitionerProfileDTO> {
    return this.http.get<PractitionerProfileDTO>(`${this.base}/practitioners/${id}`);
  }

  listByOrganization(organizationId: number): Observable<PractitionerProfileDTO[]> {
    return this.http.get<PractitionerProfileDTO[]>(
      `${this.base}/practitioners/by-organization/${organizationId}`,
    );
  }

  update(id: number, body: Partial<PractitionerProfileDTO>): Observable<PractitionerProfileDTO> {
    return this.http.put<PractitionerProfileDTO>(`${this.base}/practitioners/${id}`, body);
  }

  updateMe(body: Partial<PractitionerProfileDTO>): Observable<PractitionerProfileDTO> {
    return this.http.put<PractitionerProfileDTO>(`${this.base}/practitioners/me`, body);
  }

  uploadPhoto(file: File): Observable<PractitionerProfileDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<PractitionerProfileDTO>(`${this.base}/practitioners/me/photo`, formData);
  }

  setVerification(
    id: number,
    type: 'identite' | 'droit-exercer',
    status: VerificationStatus,
    docUrl?: string,
  ): Observable<PractitionerProfileDTO> {
    return this.http.put<PractitionerProfileDTO>(
      `${this.base}/practitioners/${id}/verifications/${type}`,
      { status, docUrl },
    );
  }

  // ── Cabinet ────────────────────────────────────────────────────────────

  myCabinet(): Observable<MedicalOrganizationDTO> {
    return this.http.get<MedicalOrganizationDTO>(`${this.base}/cabinets/me`);
  }

  getCabinet(id: number): Observable<MedicalOrganizationDTO> {
    return this.http.get<MedicalOrganizationDTO>(`${this.base}/cabinets/${id}`);
  }

  updateCabinet(
    id: number,
    body: Partial<MedicalOrganizationDTO>,
  ): Observable<MedicalOrganizationDTO> {
    return this.http.put<MedicalOrganizationDTO>(`${this.base}/cabinets/${id}`, body);
  }

  listCabinetUsers(cabinetId: number): Observable<ProUserDTO[]> {
    return this.http.get<ProUserDTO[]>(`${this.base}/cabinets/${cabinetId}/users`);
  }

  /** Liste des cabinets (tour de contrôle — JWT admin plateforme). */
  listPlatformCabinets(): Observable<MedicalOrganizationDTO[]> {
    return this.http.get<MedicalOrganizationDTO[]>(`${this.platformAdminBase}/cabinets`);
  }

  /** Personnel d’un cabinet (tour de contrôle). */
  listPlatformCabinetUsers(cabinetId: number): Observable<ProUserDTO[]> {
    return this.http.get<ProUserDTO[]>(`${this.platformAdminBase}/cabinets/${cabinetId}/users`);
  }

  updatePlatformCabinet(id: number, body: Partial<MedicalOrganizationDTO>): Observable<MedicalOrganizationDTO> {
    return this.http.put<MedicalOrganizationDTO>(`${this.platformAdminBase}/cabinets/${id}`, body);
  }

  toggleUserActive(userId: number): Observable<void> {
    return this.http.put<void>(`${this.platformAdminBase}/users/${userId}/toggle-active`, {}, {
      headers: { 'Content-Type': 'application/json' },
    });
  }

  createCabinetUser(cabinetId: number, body: CreateProUserRequest): Observable<ProUserDTO> {
    return this.http.post<ProUserDTO>(`${this.base}/cabinets/${cabinetId}/users`, body);
  }

  /** Assistants du cabinet connecté (admin ou praticien). */
  listMyCabinetAssistants(): Observable<ProUserDTO[]> {
    return this.http.get<ProUserDTO[]>(`${this.base}/assistants/my-cabinet`);
  }

  /** Crée un assistant rattaché au cabinet du compte appelant. */
  createAssistant(body: {
    nom: string;
    prenom: string;
    email: string;
    motDePasse: string;
  }): Observable<ProUserDTO> {
    return this.http.post<ProUserDTO>(`${this.base}/assistants`, body);
  }

  desactiverUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/cabinets/users/${userId}`);
  }

  reactiverUser(userId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/cabinets/users/${userId}/reactivate`, {});
  }

  // ── Lieux de consultation ──────────────────────────────────────────────

  listLocations(practitionerId: number): Observable<ConsultationLocationDTO[]> {
    return this.http.get<ConsultationLocationDTO[]>(
      `${this.base}/locations/by-practitioner/${practitionerId}`,
    );
  }

  createLocation(
    practitionerId: number,
    body: ConsultationLocationDTO,
  ): Observable<ConsultationLocationDTO> {
    return this.http.post<ConsultationLocationDTO>(
      `${this.base}/locations/by-practitioner/${practitionerId}`,
      body,
    );
  }

  updateLocation(
    id: number,
    body: ConsultationLocationDTO,
  ): Observable<ConsultationLocationDTO> {
    return this.http.put<ConsultationLocationDTO>(`${this.base}/locations/${id}`, body);
  }

  deleteLocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/locations/${id}`);
  }

  // ── Diplômes ───────────────────────────────────────────────────────────

  listDiplomas(practitionerId: number): Observable<DiplomaDTO[]> {
    return this.http.get<DiplomaDTO[]>(
      `${this.base}/diplomas/by-practitioner/${practitionerId}`,
    );
  }

  createDiploma(practitionerId: number, body: DiplomaDTO): Observable<DiplomaDTO> {
    return this.http.post<DiplomaDTO>(
      `${this.base}/diplomas/by-practitioner/${practitionerId}`,
      body,
    );
  }

  updateDiploma(id: number, body: DiplomaDTO): Observable<DiplomaDTO> {
    return this.http.put<DiplomaDTO>(`${this.base}/diplomas/${id}`, body);
  }

  deleteDiploma(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/diplomas/${id}`);
  }

  /** Questionnaires RDV (options par spécialité) — API publique. */
  getBookingWizardOptions(specialtyCode: string): Observable<BookingWizardOptionsDTO> {
    const code = (specialtyCode || 'DEFAULT').trim();
    return this.http.get<BookingWizardOptionsDTO>(`${this.publicBase}/booking/wizard-options`, {
      params: { specialtyCode: code },
    });
  }

  /** Profil public complet du praticien (sans auth). */
  getPublicPractitionerProfile(practitionerId: number): Observable<PractitionerProfileDTO> {
    return this.http.get<PractitionerProfileDTO>(
      `${this.publicBase}/practitioners/${practitionerId}/profile`,
    );
  }

  /** Lieux de consultation publics (praticien publié). */
  getPublicPractitionerLocations(practitionerId: number): Observable<ConsultationLocationDTO[]> {
    return this.http.get<ConsultationLocationDTO[]>(
      `${this.publicBase}/practitioners/${practitionerId}/locations`,
    );
  }

  // ── Actes Médicaux / Visites ───────────────────────────────────────────

  listActs(practitionerId: number): Observable<PractitionerActDTO[]> {
    return this.http.get<PractitionerActDTO[]>(
      `${this.base}/acts/by-practitioner/${practitionerId}`,
    );
  }

  createAct(practitionerId: number, body: PractitionerActDTO): Observable<PractitionerActDTO> {
    return this.http.post<PractitionerActDTO>(
      `${this.base}/acts/by-practitioner/${practitionerId}`,
      body,
    );
  }

  updateAct(id: number, body: PractitionerActDTO): Observable<PractitionerActDTO> {
    return this.http.put<PractitionerActDTO>(`${this.base}/acts/${id}`, body);
  }

  deleteAct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/acts/${id}`);
  }

  getPublicActs(practitionerId: number): Observable<PractitionerActDTO[]> {
    return this.http.get<PractitionerActDTO[]>(
      `${this.publicBase}/practitioners/${practitionerId}/acts`,
    );
  }
}

export interface PractitionerSearchResult {
  practitionerId: number;
  nom: string;
  specialty: string;
  ville: string;
  /** Code spécialité (référentiel) pour questionnaires / créneaux dynamiques. */
  primarySpecialtyCode?: string | null;
  /** URL de la photo de profil du praticien. */
  photoUrl?: string | null;
  /** Adresse du cabinet (organisation). */
  adresse?: string | null;
  globalRating?: number;
  reviewCount?: number;
  consultationFee?: number | null;
  hasMultipleLocations?: boolean;
}

export interface BookingWizardChoiceDTO {
  step: 'PRIOR_CARE' | 'VISIT_REASON';
  code: string;
  labelFr: string;
  sortOrder: number;
}

export interface BookingWizardOptionsDTO {
  requestedSpecialtyCode: string;
  resolvedSpecialtyCode: string;
  choicesPrior: BookingWizardChoiceDTO[];
  choicesVisit: BookingWizardChoiceDTO[];
}
