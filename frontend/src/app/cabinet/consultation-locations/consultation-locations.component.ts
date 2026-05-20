import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { take } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { AuthProService } from '../../services/auth-pro.service';
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
          this.errorMessage = 'Impossible de charger votre profil praticien.';
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
        error: (e) => (this.errorMessage = e.error?.error || 'Erreur de chargement'),
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
    this.editing.horaires.push({
      jour: 'LUNDI',
      heureDebut: '09:00',
      heureFin: '12:00',
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
      this.editingError = "Le nom de l'établissement et l'adresse sont obligatoires";
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
        this.successMessage = 'Lieu enregistré';
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.editingError = e.error?.error || "Erreur d'enregistrement";
      },
    });
  }

  remove(loc: ConsultationLocationDTO) {
    if (!loc.id) return;
    if (!confirm(`Supprimer le lieu « ${loc.nomEtablissement} » ?`)) return;
    this.practitionerService.deleteLocation(loc.id).subscribe({
      next: () => {
        this.successMessage = 'Lieu supprimé';
        this.refresh();
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
}
