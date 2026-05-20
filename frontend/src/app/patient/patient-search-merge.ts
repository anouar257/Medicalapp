import type { AgendaDoctorListDTO } from '../services/agenda.service';
import type { PractitionerSearchResult } from '../services/practitioner.service';

/** Ligne unifiée : médecins agenda (admin) + annuaire practitioner-service. */
export interface CombinedPractitionerOption {
  key: string;
  nom: string;
  specialty: string;
  ville: string;
  practitionerId: number | null;
  agendaDoctorId: number | null;
  sourceTag: 'agenda' | 'directory' | 'both';
  /** Code spécialité métier (questionnaires dynamiques). */
  specialtyCode: string | null;
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
        nom: match.nom?.trim() || d.name,
        specialty: match.specialty?.trim() || d.specialty || '',
        ville: match.ville || '',
        practitionerId: ext,
        agendaDoctorId: d.id,
        sourceTag: 'both',
        specialtyCode: match.primarySpecialtyCode?.trim() || d.specialtyCode?.trim() || null,
      });
    } else {
      out.push({
        key: `ag-${d.id}`,
        nom: d.name,
        specialty: d.specialty ?? '',
        ville: '',
        practitionerId: ext,
        agendaDoctorId: d.id,
        sourceTag: 'agenda',
        specialtyCode: d.specialtyCode?.trim() || null,
      });
    }
  }

  for (const p of pros) {
    if (usedProIds.has(p.practitionerId)) continue;
    out.push({
      key: `pr-${p.practitionerId}`,
      nom: p.nom,
      specialty: p.specialty,
      ville: p.ville,
      practitionerId: p.practitionerId,
      agendaDoctorId: null,
      sourceTag: 'directory',
      specialtyCode: p.primarySpecialtyCode?.trim() || null,
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
    if (c && !r.ville.toLowerCase().includes(c)) return false;
    if (s && !r.specialty.toLowerCase().includes(s)) return false;
    return true;
  });
}
