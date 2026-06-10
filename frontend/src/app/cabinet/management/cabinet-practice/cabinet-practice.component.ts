import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../../services/practitioner.service';
import { AuthProService } from '../../../services/auth-pro.service';
import { PreferencesService } from '../../../services/preferences.service';
import { AgendaStateService } from '../../../services/agenda-state.service';
import { MedicalOrganizationDTO, ConsultationLocationDTO, HoraireDTO, JourSemaine } from '../../../models/practitioner.model';

type PracticeLoad = { kind: 'ok'; cabinet: MedicalOrganizationDTO } | { kind: 'no-org' } | { kind: 'error' };

/** Fiche et coordonnées du cabinet (praticien) — avec adresses multi-sites et horaires associés. */
@Component({
  selector: 'app-cabinet-practice',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cabinet-practice.component.html',
  styleUrls: ['./cabinet-practice.component.scss'],
})
export class CabinetPracticeComponent {
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly authPro = inject(AuthProService);
  private readonly agendaState = inject(AgendaStateService);
  private readonly destroyRef = inject(DestroyRef);

  cabinet: MedicalOrganizationDTO | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';

  practitionerId: number | null = null;
  locations: ConsultationLocationDTO[] = [];
  editing: ConsultationLocationDTO | null = null;
  editingError = '';
  editingSaving = false;

  readonly jours: JourSemaine[] = ['LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'];
  readonly hourOptions = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, '0'));
  readonly minuteOptions = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'];

  getHour(timeStr: string | null | undefined): string {
    if (!timeStr) return '09';
    return timeStr.split(':')[0] || '09';
  }

  getMinute(timeStr: string | null | undefined): string {
    if (!timeStr) return '00';
    return timeStr.split(':')[1] || '00';
  }

  setHour(h: HoraireDTO, field: 'heureDebut' | 'heureFin', hour: string): void {
    const current = h[field] || '00:00';
    const parts = current.split(':');
    const minute = parts[1] || '00';
    h[field] = `${hour}:${minute}`;
  }

  setMinute(h: HoraireDTO, field: 'heureDebut' | 'heureFin', minute: string): void {
    const current = h[field] || '00:00';
    const parts = current.split(':');
    const hour = parts[0] || '00';
    h[field] = `${hour}:${minute}`;
  }

  constructor() {
    toObservable(this.authPro.organizationId)
      .pipe(
        distinctUntilChanged(),
        switchMap((orgId): Observable<PracticeLoad> => {
          if (!orgId) {
            return of({ kind: 'no-org' as const });
          }
          this.loading = true;
          this.errorMessage = '';
          return this.practitionerService.getCabinet(orgId).pipe(
            map((cabinet) => ({ kind: 'ok' as const, cabinet })),
            catchError(() => of({ kind: 'error' as const })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        if (r.kind === 'no-org') {
          this.cabinet = null;
          this.loading = false;
          this.errorMessage = this.prefs.translate('cabinet.practice.noOrganization');
          return;
        }
        if (r.kind === 'error') {
          this.cabinet = null;
          this.loading = false;
          this.errorMessage = this.prefs.translate('cabinet.practice.loadError');
          return;
        }
        this.cabinet = r.cabinet;
        this.loading = false;
        this.errorMessage = '';
      });

    // Load practitionerId and fetch their consultation locations
    toObservable(this.authPro.workspaceContextKey)
      .pipe(
        distinctUntilChanged(),
        switchMap(() =>
          this.practitionerService.me().pipe(
            map((p) => p.id),
            catchError(() => {
              const fid = this.authPro.practitionerProfileId();
              return fid != null ? of(fid) : of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((id) => {
        this.practitionerId = id;
        if (id != null) {
          this.refreshLocations();
        }
      });
  }

  refreshLocations() {
    if (!this.practitionerId) return;
    this.practitionerService
      .listLocations(this.practitionerId)
      .subscribe({
        next: (l) => (this.locations = l),
        error: (e) => (this.errorMessage = e.error?.error || 'Erreur de chargement des adresses'),
      });
  }

  newLocation() {
    this.editing = {
      nomEtablissement: '',
      adresse: '',
      ascenseur: false,
      entreeAccessible: false,
      parking: 'AUCUN',
      actif: true,
      horaires: [],
    };
    this.editingError = '';
  }

  edit(loc: ConsultationLocationDTO) {
    this.editing = JSON.parse(JSON.stringify(loc));
    this.editingError = '';
  }

  cancelEdit() {
    this.editing = null;
    this.editingError = '';
  }

  addHoraire() {
    if (!this.editing) return;
    
    let nextDay: JourSemaine = 'LUNDI';
    let nextStart = '09:00';
    let nextEnd = '12:00';
    
    const count = this.editing.horaires.length;
    if (count > 0) {
      const last = this.editing.horaires[count - 1];
      nextDay = last.jour;
      
      const daySlots = this.editing.horaires.filter((h) => h.jour === nextDay);
      if (daySlots.length === 1) {
        nextStart = '14:00';
        nextEnd = '18:00';
      } else {
        const dayIndex = this.jours.indexOf(last.jour);
        if (dayIndex !== -1 && dayIndex < this.jours.length - 1) {
          nextDay = this.jours[dayIndex + 1];
        }
        nextStart = '09:00';
        nextEnd = '12:00';
      }
    }
    
    this.editing.horaires.push({
      jour: nextDay,
      heureDebut: nextStart,
      heureFin: nextEnd,
      continu: false,
    } as HoraireDTO);
  }

  removeHoraire(i: number) {
    if (!this.editing) return;
    this.editing.horaires.splice(i, 1);
  }

  saveLocation() {
    if (!this.editing || !this.practitionerId) return;
    this.editingError = '';
    if (!this.editing.nomEtablissement?.trim() || !this.editing.adresse?.trim()) {
      this.editingError = "Le nom de l'établissement et l'adresse sont obligatoires";
      return;
    }
    this.editingSaving = true;
    const obs = this.editing.id
      ? this.practitionerService.updateLocation(this.editing.id, this.editing)
      : this.practitionerService.createLocation(this.practitionerId, this.editing);
    obs.subscribe({
      next: () => {
        this.editingSaving = false;
        this.editing = null;
        this.successMessage = 'Adresse enregistrée avec succès';
        this.refreshLocations();
        this.agendaState.refreshCabinetHoraires();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.editingSaving = false;
        this.editingError = e.error?.error || "Erreur d'enregistrement";
      },
    });
  }

  removeLocation(loc: ConsultationLocationDTO) {
    if (!loc.id) return;
    if (!confirm(`Supprimer l'adresse « ${loc.nomEtablissement} » ?`)) return;
    this.practitionerService.deleteLocation(loc.id).subscribe({
      next: () => {
        this.successMessage = 'Adresse supprimée avec succès';
        this.refreshLocations();
        this.agendaState.refreshCabinetHoraires();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => (this.errorMessage = e.error?.error || 'Erreur de suppression'),
    });
  }

  jourLabel(j: JourSemaine | string): string {
    return j.charAt(0) + j.slice(1).toLowerCase();
  }

  parkingLabel(p: string): string {
    switch (p) {
      case 'GRATUIT':
        return 'Parking gratuit';
      case 'PAYANT':
        return 'Parking payant';
      default:
        return 'Aucun parking';
    }
  }

  urgenceLabel(u: string): string {
    switch (u) {
      case 'SECRETARIAT':
        return 'Secrétariat';
      case 'SOS_MEDECINS':
        return 'SOS Médecins';
      case 'NUMERO_PERSONNEL':
        return 'Numéro personnel';
      case 'NUMERO_DIRECT':
        return 'Numéro direct';
      default:
        return u;
    }
  }

  save() {
    if (!this.cabinet) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.practitionerService.updateCabinet(this.cabinet.id, this.cabinet).subscribe({
      next: (c) => {
        this.cabinet = c;
        this.saving = false;
        this.successMessage = this.prefs.translate('cabinet.practice.saveSuccess');
        const u = this.authPro.getCurrentUser();
        if (u && c.nom) {
          this.authPro.patchCurrentUser({ organizationNom: c.nom });
        }
        
        this.agendaState.refreshCabinetHoraires();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.errorMessage =
          typeof e.error?.error === 'string' && e.error.error
            ? e.error.error
            : this.prefs.translate('cabinet.practice.saveError');
      },
    });
  }
}
