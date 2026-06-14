import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of, take } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { AuthProService } from '../../services/auth-pro.service';
import { PreferencesService } from '../../services/preferences.service';
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
  readonly prefs = inject(PreferencesService);
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
          this.errorMessage = this.prefs.translate('PRACTITIONER.DIPLOMAS.ERROR_PROFILE_MISSING');
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
        error: (e) => (this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.DIPLOMAS.ERROR_LOADING')),
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
      this.editingError = this.prefs.translate('PRACTITIONER.DIPLOMAS.TITLE_REQUIRED');
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
        this.successMessage = this.prefs.translate('PRACTITIONER.DIPLOMAS.SUCCESS_SAVED');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.editingError = e.error?.error || this.prefs.translate('PRACTITIONER.DIPLOMAS.ERROR_SAVE');
      },
    });
  }

  remove(d: DiplomaDTO) {
    if (!d.id) return;
    if (!confirm(this.prefs.translate('PRACTITIONER.DIPLOMAS.DELETE_CONFIRM').replace('{title}', d.intitule))) return;
    this.practitionerService.deleteDiploma(d.id).subscribe({
      next: () => {
        this.successMessage = this.prefs.translate('PRACTITIONER.DIPLOMAS.SUCCESS_DELETED');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => (this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.DIPLOMAS.ERROR_DELETE')),
    });
  }

  typeLabel(t: DiplomaType): string {
    switch (t) {
      case 'DIPLOME':
        return this.prefs.translate('PRACTITIONER.DIPLOMAS.DIPLOMA');
      case 'CERTIFICATION':
        return this.prefs.translate('PRACTITIONER.DIPLOMAS.CERTIFICATION');
      case 'CONFERENCE':
        return this.prefs.translate('PRACTITIONER.DIPLOMAS.CONFERENCE');
      case 'FORMATION':
        return this.prefs.translate('PRACTITIONER.DIPLOMAS.TRAINING');
    }
  }
}
