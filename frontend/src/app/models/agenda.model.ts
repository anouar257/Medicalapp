export interface Doctor {
  id: string;
  name: string;
  colorCode: string;
  /** URL de la photo du médecin (obligatoire — fallback initiales si vide). */
  photoUrl: string;
  /** Spécialité médicale (ex. Cardiologie). */
  specialty?: string;
  /** Nombre de RDV en base (API liste médecins). */
  appointmentCount?: number;
  /** Identifiant unique du praticien dans practitioner-service. */
  externalPractitionerId?: number;
}

/**
 * Type de visite — dynamique, alimenté par la table `appointment_types` côté backend.
 * Le `code` reste stable pour les filtres (ex. `CONSULTATION`, `CONTROL`), tandis que `label`
 * et `colorCode` sont éditables côté admin.
 */
export interface AppointmentType {
  id: string;
  code: string;
  label: string;
  colorCode: string;
  defaultDurationMinutes: number;
  displayOrder: number;
  active: boolean;
  price?: number | null;
  priceVariable?: boolean;
  sourcePractitionerId?: number | null;
  sourceActId?: number | null;
  doctorName?: string;
}

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface Appointment {
  id: string;
  title: string;
  /** Identifiant de l'`AppointmentType` (FK côté backend). */
  typeId: string;
  /** Code stable copié depuis l'`AppointmentType` (utile pour filtres / persistance). */
  typeCode: string;
  /** Libellé pour affichage badge (ex. "Consultation"). */
  typeLabel: string;
  /** Couleur badge (hex) provenant de l'`AppointmentType`. */
  typeColor: string;
  startTime: Date;
  endTime: Date;
  durationMinutes: number;
  description: string;
  /** Motif dynamique choisi dans le wizard patient (souvent le vrai acte praticien). */
  visitReasonCode?: string;
  doctorId: string;
  color: string;
  /** Utile surtout pour RDV annulés (barré dans le calendrier). */
  status?: AppointmentStatus;
  /** Mode de consultation */
  locationMode?: 'CABINET' | 'CLINIC' | 'REMOTE' | string;
}

/** Vue affichée par le calendrier — détermine la fenêtre temporelle autour de `selectedDate`. */
export type AgendaView = 'day' | 'week' | 'month' | 'year';
