/**
 * Types alignés sur les enums du practitioner-service.
 */

/** Rôles comptes professionnels (practitioner-service). */
export type ProUserRole = 'ADMIN' | 'PRATICIEN' | 'ASSISTANT';

export type Civilite = 'M' | 'MME';
export type Titre = 'AUCUN' | 'DR' | 'PR';
export type SexePro = 'HOMME' | 'FEMME';

export type StatutPraticien =
  | 'REMPLACANT'
  | 'COLLABORATEUR'
  | 'ASSISTANT'
  | 'INTERNE'
  | 'ASSOCIE'
  | 'TITULAIRE'
  | 'INDISPONIBLE';

export type ParkingType = 'AUCUN' | 'GRATUIT' | 'PAYANT';

export type ContactUrgenceType =
  | 'SECRETARIAT'
  | 'SOS_MEDECINS'
  | 'NUMERO_PERSONNEL'
  | 'NUMERO_DIRECT';

export type VerificationStatus = 'NON_FOURNI' | 'EN_ATTENTE' | 'VERIFIE' | 'REFUSE';

export type JourSemaine =
  | 'LUNDI'
  | 'MARDI'
  | 'MERCREDI'
  | 'JEUDI'
  | 'VENDREDI'
  | 'SAMEDI'
  | 'DIMANCHE';

export type DiplomaType = 'DIPLOME' | 'CERTIFICATION' | 'CONFERENCE' | 'FORMATION';

// ── Auth ──────────────────────────────────────────────────────────────────

export interface LoginProRequest {
  email: string;
  motDePasse: string;
}

export interface AuthProResponse {
  token: string;
  userId: number;
  email: string;
  telephone: string;
  prenom: string;
  nom: string;
  role: ProUserRole;
  organizationId: number | null;
  organizationNom: string | null;
  emailVerifie: boolean;
  telephoneVerifie: boolean;
  practitionerProfileId: number | null;
}

export interface VerifyOtpProApiResponse {
  verified: boolean;
  message?: string;
  error?: string;
  token?: string;
  userId?: number;
  email?: string;
  telephone?: string;
  prenom?: string;
  nom?: string;
  role?: ProUserRole;
  organizationId?: number | null;
  organizationNom?: string | null;
  emailVerifie?: boolean;
  telephoneVerifie?: boolean;
  practitionerProfileId?: number | null;
}

export interface RegisterCabinetRequest {
  nomCabinet: string;
  siret?: string;
  adresseCabinet?: string;
  villeCabinet?: string;
  codePostalCabinet?: string;
  telephoneCabinet?: string;
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
  motDePasse: string;
  cguAcceptees: boolean;
}

export interface RegisterPractitionerRequest {
  civilite?: Civilite;
  titre: Titre;
  sexe: SexePro;
  prenom: string;
  nom: string;
  dateNaissance: string;
  email: string;
  telephone: string;
  motDePasse: string;
  cguAcceptees: boolean;
  statut?: StatutPraticien;
  specialiteIds?: number[];
  organizationId?: number | null;
  nomCabinetPersonnel?: string;
}

// ── Catalogues ────────────────────────────────────────────────────────────

export interface SpecialtyDTO {
  id: number;
  code: string;
  libelle: string;
  description?: string;
}

// ── Profil praticien ──────────────────────────────────────────────────────

export interface DiplomaDTO {
  id?: number;
  intitule: string;
  etablissement?: string;
  anneeObtention?: number | null;
  dateObtention?: string | null;
  type: DiplomaType;
  documentUrl?: string;
}

export interface PractitionerProfileDTO {
  id: number;
  proUserId: number;
  email: string;
  telephone: string;
  civilite?: Civilite;
  titre: Titre;
  sexe?: SexePro;
  prenom: string;
  nom: string;
  dateNaissance?: string;
  statut: StatutPraticien;
  empreinte?: string;
  lienYoutube?: string;
  siteWeb?: string;
  biographie?: string;
  photoUrl?: string;
  colorCode?: string;
  verifIdentiteStatus: VerificationStatus;
  verifDroitExercerStatus: VerificationStatus;
  disponible: boolean;
  specialites: SpecialtyDTO[];
  diplomes: DiplomaDTO[];
  organizationId?: number;
  organizationNom?: string;
}

// ── Lieux de consultation ─────────────────────────────────────────────────

export interface HoraireDTO {
  id?: number;
  jour: JourSemaine;
  heureDebut: string; // HH:mm
  heureFin: string;
  continu: boolean;
}

export interface ConsultationLocationDTO {
  id?: number;
  practitionerId?: number;
  nomEtablissement: string;
  adresse: string;
  ville?: string;
  codePostal?: string;
  pays?: string;
  telephoneBureau?: string;
  fax?: string;
  ascenseur: boolean;
  entreeAccessible: boolean;
  etage?: string;
  parking: ParkingType;
  contactUrgenceType?: ContactUrgenceType;
  telephoneUrgence?: string;
  actif: boolean;
  horaires: HoraireDTO[];
}

// ── Cabinet ───────────────────────────────────────────────────────────────

/** Patient côté tour de contrôle (données issues de patient-service via practitioner-service). */
export interface PlatformPatientDTO {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
  actif: boolean;
  dateInscription?: string;
}

export interface MedicalOrganizationDTO {
  id: number;
  nom: string;
  siret?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  ville?: string;
  codePostal?: string;
  pays?: string;
  actif: boolean;
}

export interface ProUserDTO {
  id: number;
  email: string;
  telephone: string;
  prenom: string;
  nom: string;
  role: ProUserRole;
  emailVerifie: boolean;
  telephoneVerifie: boolean;
  actif: boolean;
  organizationId?: number;
  practitionerProfileId?: number;
}

export interface CreateProUserRequest {
  email: string;
  telephone: string;
  prenom: string;
  nom: string;
  role: ProUserRole;
  motDePasse?: string;
}

// ── OTP / mot de passe ────────────────────────────────────────────────────

export interface SendOtpProRequest {
  to: string;
  channel: 'sms' | 'email';
}

export interface VerifyOtpProRequest {
  to: string;
  code: string;
  channel: 'sms' | 'email';
}

export interface ForgotPasswordProRequest {
  identifiant: string;
  channel: 'email' | 'sms';
}

export interface ResetPasswordProRequest {
  token: string;
  nouveauMotDePasse: string;
}
