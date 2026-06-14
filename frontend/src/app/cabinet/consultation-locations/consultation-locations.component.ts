import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { take } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { AuthProService } from '../../services/auth-pro.service';
import { PreferencesService } from '../../services/preferences.service';
import {
  ConsultationLocationDTO,
  HoraireDTO,
  JourSemaine,
} from '../../models/practitioner.model';

const JOURS: JourSemaine[] = ['LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'];

/**
 * Gestion des lieux de consultation du praticien connecté.
 *
 * <p>Couvre tous les champs du cahier des charges : adresse, accessibilité (ascenseur,
 * entrée), étage, parking, horaires d'ouverture par jour (matin / après-midi / continu),
 * contact d'urgence (type + téléphone).
 */
@Component({
  selector: 'app-consultation-locations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './consultation-locations.component.html',
  styleUrls: ['./consultation-locations.component.scss'],
})
export class ConsultationLocationsComponent {
  readonly prefs = inject(PreferencesService);
  practitionerId: number | null = null;
  locations: ConsultationLocationDTO[] = [];
  editing: ConsultationLocationDTO | null = null;
  editingError = '';
  saving = false;
  errorMessage = '';
  successMessage = '';

  jours: JourSemaine[] = JOURS;

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private practitionerService: PractitionerService,
    private authPro: AuthProService,
  ) {
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
        if (id == null) {
          this.errorMessage = this.prefs.translate('PRACTITIONER.LOCATIONS.ERROR_PROFILE_MISSING');
          this.locations = [];
          return;
        }
        this.errorMessage = '';
        this.refresh();
      });
  }

  refresh() {
    if (!this.practitionerId) return;
    this.practitionerService
      .listLocations(this.practitionerId)
      .pipe(take(1))
      .subscribe({
        next: (l) => (this.locations = l),
        error: (e) => (this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.LOCATIONS.ERROR_LOADING')),
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
      
      // Compter le nombre de plages déjà existantes pour ce jour spécifique
      const daySlots = this.editing.horaires.filter((h) => h.jour === nextDay);
      if (daySlots.length === 1) {
        // S'il existe déjà une plage (le matin), la nouvelle se pré-remplit automatiquement avec 14:00 - 18:00
        nextStart = '14:00';
        nextEnd = '18:00';
      } else {
        // Sinon, on passe au jour de la semaine suivant et on pré-remplit avec 09:00 - 12:00
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

  save() {
    if (!this.editing || !this.practitionerId) return;
    this.editingError = '';
    if (!this.editing.nomEtablissement?.trim() || !this.editing.adresse?.trim()) {
      this.editingError = this.prefs.translate('PRACTITIONER.LOCATIONS.REQUIRED_FIELDS');
      return;
    }
    this.saving = true;
    const obs = this.editing.id
      ? this.practitionerService.updateLocation(this.editing.id, this.editing)
      : this.practitionerService.createLocation(this.practitionerId, this.editing);
    obs.subscribe({
      next: () => {
        this.saving = false;
        this.editing = null;
        this.successMessage = this.prefs.translate('PRACTITIONER.LOCATIONS.SUCCESS_SAVED');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.editingError = e.error?.error || this.prefs.translate('PRACTITIONER.LOCATIONS.ERROR_SAVE');
      },
    });
  }

  remove(loc: ConsultationLocationDTO) {
    if (!loc.id) return;
    if (!confirm(this.prefs.translate('PRACTITIONER.LOCATIONS.DELETE_CONFIRM').replace('{name}', loc.nomEtablissement))) return;
    this.practitionerService.deleteLocation(loc.id).subscribe({
      next: () => {
        this.successMessage = this.prefs.translate('PRACTITIONER.LOCATIONS.SUCCESS_DELETED');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => (this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.LOCATIONS.ERROR_DELETE')),
    });
  }

  jourLabel(j: JourSemaine | string): string {
    return this.prefs.translate(`COMMON.DAYS.${j}`);
  }

  parkingLabel(p: string): string {
    switch (p) {
      case 'GRATUIT':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.PARKING_FREE');
      case 'PAYANT':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.PARKING_PAID');
      default:
        return this.prefs.translate('PRACTITIONER.LOCATIONS.NO_PARKING');
    }
  }

  urgenceLabel(u: string): string {
    switch (u) {
      case 'SECRETARIAT':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.SECRETARIAT');
      case 'SOS_MEDECINS':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.SOS_MEDECINS');
      case 'NUMERO_PERSONNEL':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.PERSONAL_NUMBER');
      case 'NUMERO_DIRECT':
        return this.prefs.translate('PRACTITIONER.LOCATIONS.DIRECT_NUMBER');
      default:
        return u;
    }
  }
}
