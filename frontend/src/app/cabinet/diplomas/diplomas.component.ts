import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of, take } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { AuthProService } from '../../services/auth-pro.service';
import { DiplomaDTO, DiplomaType, PractitionerProfileDTO } from '../../models/practitioner.model';

/**
 * Diplômes, certifications, conférences, formations.
 * Cf. cahier : « Vérifier ses diplômes + certifications + conférences + youtube + ... ».
 */
@Component({
  selector: 'app-diplomas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './diplomas.component.html',
  styleUrls: ['./diplomas.component.scss'],
})
export class DiplomasComponent {
  practitionerId: number | null = null;
  diplomas: DiplomaDTO[] = [];
  editing: DiplomaDTO | null = null;
  editingError = '';
  saving = false;
  errorMessage = '';
  successMessage = '';

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
            map((p: PractitionerProfileDTO) => p.id),
            catchError(() => {
              const fid = this.authPro.practitionerProfileId();
              return fid != null ? of<number>(fid) : of<number | null>(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((id) => {
        this.practitionerId = id;
        if (id == null) {
          this.errorMessage = 'Profil praticien introuvable.';
          this.diplomas = [];
          return;
        }
        this.errorMessage = '';
        this.refresh();
      });
  }

  refresh() {
    if (!this.practitionerId) return;
    this.practitionerService
      .listDiplomas(this.practitionerId)
      .pipe(take(1))
      .subscribe({
        next: (d) => (this.diplomas = d),
        error: (e) => (this.errorMessage = e.error?.error || 'Erreur de chargement'),
      });
  }

  newDiploma() {
    this.editing = {
      intitule: '',
      type: 'DIPLOME' as DiplomaType,
    };
    this.editingError = '';
  }

  edit(d: DiplomaDTO) {
    this.editing = { ...d };
    this.editingError = '';
  }

  cancelEdit() {
    this.editing = null;
    this.editingError = '';
  }

  save() {
    if (!this.editing || !this.practitionerId) return;
    if (!this.editing.intitule?.trim()) {
      this.editingError = "L'intitulé est obligatoire";
      return;
    }
    this.saving = true;
    const obs = this.editing.id
      ? this.practitionerService.updateDiploma(this.editing.id, this.editing)
      : this.practitionerService.createDiploma(this.practitionerId, this.editing);
    obs.subscribe({
      next: () => {
        this.saving = false;
        this.editing = null;
        this.successMessage = 'Enregistré';
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.editingError = e.error?.error || "Erreur d'enregistrement";
      },
    });
  }

  remove(d: DiplomaDTO) {
    if (!d.id) return;
    if (!confirm(`Supprimer « ${d.intitule} » ?`)) return;
    this.practitionerService.deleteDiploma(d.id).subscribe({
      next: () => {
        this.successMessage = 'Supprimé';
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => (this.errorMessage = e.error?.error || 'Erreur'),
    });
  }

  typeLabel(t: DiplomaType): string {
    switch (t) {
      case 'DIPLOME':
        return 'Diplôme';
      case 'CERTIFICATION':
        return 'Certification';
      case 'CONFERENCE':
        return 'Conférence';
      case 'FORMATION':
        return 'Formation';
    }
  }
}
