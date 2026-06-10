import type { AgendaDoctorListDTO } from '../services/agenda.service';
import type { PractitionerSearchResult } from '../services/practitioner.service';

/** Ligne unifiée : médecins agenda (admin) + annuaire practitioner-service. */
export interface CombinedPractitionerOption {
  key: string;
  nom: string;
  specialty: string;
  ville: string;
  /** Adresse du cabinet (organisation). */
  adresse: string;
  practitionerId: number | null;
  agendaDoctorId: number | null;
  sourceTag: 'agenda' | 'directory' | 'both';
  /** Code spécialité métier (questionnaires dynamiques). */
  specialtyCode: string | null;
  /** URL de la photo de profil du praticien. */
  photoUrl: string;
  consultationFee?: number | null;
  globalRating?: number | null;
  reviewCount?: number | null;
  hasMultipleLocations?: boolean;
}

/**
 * Assure un affichage propre sans redondance (ex: supprime "Dr. Dr." ou "Dr ").
 */
function formatDoctorName(name: string): string {
  const cleaned = name.replace(/^(Dr\.?\s*|Pr\.?\s*)+/ig, '').trim();
  return 'Dr. ' + cleaned;
}

export function mergePractitionerSearchResults(
  pros: PractitionerSearchResult[],
  doctors: AgendaDoctorListDTO[],
): CombinedPractitionerOption[] {
  const usedProIds = new Set<number>();
  const out: CombinedPractitionerOption[] = [];

  for (const d of doctors) {
    const ext = d.externalPractitionerId ?? null;
    const match = ext != null ? pros.find((p) => p.practitionerId === ext) : undefined;
    if (match) {
      usedProIds.add(match.practitionerId);
      out.push({
        key: `sync-${ext}`,
        nom: formatDoctorName(match.nom?.trim() || d.name),
        specialty: match.specialty?.trim() || d.specialty || '',
        ville: match.ville || '',
        adresse: match.adresse?.trim() || '',
        practitionerId: ext,
        agendaDoctorId: d.id,
        sourceTag: 'both',
        specialtyCode: match.primarySpecialtyCode?.trim() || d.specialtyCode?.trim() || null,
        photoUrl: match.photoUrl?.trim() || d.photoUrl || '',
        consultationFee: match.consultationFee ?? null,
        globalRating: match.globalRating ?? null,
        reviewCount: match.reviewCount ?? null,
        hasMultipleLocations: match.hasMultipleLocations ?? false,
      });
    } else {
      out.push({
        key: `ag-${d.id}`,
        nom: formatDoctorName(d.name),
        specialty: d.specialty ?? '',
        ville: '',
        adresse: '',
        practitionerId: ext,
        agendaDoctorId: d.id,
        sourceTag: 'agenda',
        specialtyCode: d.specialtyCode?.trim() || null,
        photoUrl: d.photoUrl || '',
        consultationFee: null,
        globalRating: null,
        reviewCount: null,
        hasMultipleLocations: false,
      });
    }
  }

  for (const p of pros) {
    if (usedProIds.has(p.practitionerId)) continue;
    out.push({
      key: `pr-${p.practitionerId}`,
      nom: formatDoctorName(p.nom),
      specialty: p.specialty,
      ville: p.ville,
      adresse: p.adresse?.trim() || '',
      practitionerId: p.practitionerId,
      agendaDoctorId: null,
      sourceTag: 'directory',
      specialtyCode: p.primarySpecialtyCode?.trim() || null,
      photoUrl: p.photoUrl?.trim() || '',
      consultationFee: p.consultationFee ?? null,
      globalRating: p.globalRating ?? null,
      reviewCount: p.reviewCount ?? null,
      hasMultipleLocations: p.hasMultipleLocations ?? false,
    });
  }

  return out;
}

export function filterCombinedOptions(
  rows: CombinedPractitionerOption[],
  name: string,
  city: string,
  specialtyToken: string,
): CombinedPractitionerOption[] {
  const n = name.trim().toLowerCase();
  const c = city.trim().toLowerCase();
  const s = specialtyToken.trim().toLowerCase();
  return rows.filter((r) => {
    if (n && !r.nom.toLowerCase().includes(n)) return false;
    
    // Check city against both ville and adresse
    if (c) {
      const matchVille = r.ville.toLowerCase().includes(c);
      const matchAdresse = r.adresse && r.adresse.toLowerCase().includes(c);
      if (!matchVille && !matchAdresse) return false;
    }
    
    if (s && !r.specialty.toLowerCase().includes(s)) return false;
    return true;
  });
}
