import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { PractitionerProfileDTO, SpecialtyDTO, PractitionerActDTO } from '../../models/practitioner.model';
import { PreferencesService } from '../../services/preferences.service';
import { AuthProService } from '../../services/auth-pro.service';
import { resolveDoctorPhotoUrl } from '../../utils/media-url';

interface ProfileBundle {
  profile: PractitionerProfileDTO | null;
  specialties: SpecialtyDTO[];
  loadError?: string;
}

/**
 * Profil détaillé du praticien connecté — édition de tous les champs du cahier
 * des charges (civilité, titre, statut, spécialités, biographie, liens, couleur).
 */
@Component({
  selector: 'app-practitioner-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './practitioner-profile.component.html',
  styleUrls: ['./practitioner-profile.component.scss'],
})
export class PractitionerProfileComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);
  private readonly destroyRef = inject(DestroyRef);
  readonly ratingStars = [1, 2, 3, 4, 5];

  profile: PractitionerProfileDTO | null = null;
  specialties: SpecialtyDTO[] = [];
  saving = false;
  errorMessage = '';
  successMessage = '';

  acts: PractitionerActDTO[] = [];
  showActForm = false;
  editingAct: PractitionerActDTO | null = null;

  constructor(private practitionerService: PractitionerService) {
    toObservable(this.authPro.workspaceContextKey)
      .pipe(
        distinctUntilChanged(),
        switchMap(() =>
          forkJoin({
              profile: this.practitionerService.me(),
            specialties: this.practitionerService.listSpecialties(),
          }).pipe(
            catchError((e: unknown) => {
              const msg =
                (e as { error?: { error?: string } })?.error?.error ??
                this.prefs.translate('PRACTITIONER.PROFILE.NOT_FOUND');
              return of<ProfileBundle>({
                profile: null,
                specialties: [],
                loadError: typeof msg === 'string' ? msg : this.prefs.translate('PRACTITIONER.PROFILE.NOT_FOUND'),
              });
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res: ProfileBundle | { profile: PractitionerProfileDTO; specialties: SpecialtyDTO[] }) => {
        const bundle = res as ProfileBundle;
        this.profile = bundle.profile;
        this.specialties = bundle.specialties ?? [];
        this.errorMessage = bundle.loadError ?? '';
        if (this.profile) {
          this.loadActs(this.profile.id);
        }
      });
  }

  loadActs(practitionerId: number) {
    this.practitionerService.listActs(practitionerId).subscribe({
      next: (list) => {
        this.acts = list ?? [];
      },
      error: (e) => {
        console.error('Error loading acts:', e);
      }
    });
  }

  addNewAct() {
    this.editingAct = {
      name: '',
      durationMinutes: 15,
      price: null,
      isPriceVariable: false
    };
    this.showActForm = true;
  }

  editAct(act: PractitionerActDTO) {
    this.editingAct = { ...act };
    this.showActForm = true;
  }

  deleteAct(id: number | undefined) {
    if (!id) return;
    if (confirm(this.prefs.translate('PRACTITIONER.ACTS.DELETE_CONFIRM'))) {
      this.practitionerService.deleteAct(id).subscribe({
        next: () => {
          if (this.profile) {
            this.loadActs(this.profile.id);
          }
          this.successMessage = this.prefs.translate('PRACTITIONER.ACTS.DELETE_SUCCESS');
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (e) => {
          this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.ACTS.DELETE_ERROR');
          setTimeout(() => (this.errorMessage = ''), 3000);
        }
      });
    }
  }

  cancelActEdit() {
    this.editingAct = null;
    this.showActForm = false;
  }

  saveAct() {
    if (!this.editingAct || !this.profile) return;
    if (!this.editingAct.name.trim()) {
      this.errorMessage = this.prefs.translate('PRACTITIONER.ACTS.REQUIRED_NAME');
      setTimeout(() => (this.errorMessage = ''), 3000);
      return;
    }

    const payload: PractitionerActDTO = {
      ...this.editingAct,
      price: this.editingAct.isPriceVariable ? null : (this.editingAct.price != null ? Number(this.editingAct.price) : null)
    };

    const request = payload.id
      ? this.practitionerService.updateAct(payload.id, payload)
      : this.practitionerService.createAct(this.profile.id, payload);

    request.subscribe({
      next: () => {
        this.loadActs(this.profile!.id);
        this.cancelActEdit();
        this.successMessage = this.prefs.translate('PRACTITIONER.ACTS.SAVE_SUCCESS');
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.ACTS.SAVE_ERROR');
        setTimeout(() => (this.errorMessage = ''), 3000);
      }
    });
  }

  hasSpecialty(id: number): boolean {
    return !!this.profile?.specialites?.some((s) => s.id === id);
  }

  filledRating(value: number | null | undefined): number {
    if (value == null || Number.isNaN(Number(value))) {
      return 0;
    }
    return Math.round(Number(value));
  }

  toggleSpecialty(s: SpecialtyDTO) {
    if (!this.profile) return;
    const exists = this.hasSpecialty(s.id);
    if (exists) {
      this.profile.specialites = this.profile.specialites.filter((x) => x.id !== s.id);
    } else {
      this.profile.specialites = [...(this.profile.specialites ?? []), s];
    }
  }

  getPhotoUrl(url: string | undefined | null): string {
    const name = this.profile ? `${this.profile.prenom ?? ''} ${this.profile.nom ?? ''}`.trim() : '';
    return resolveDoctorPhotoUrl(url, name);
  }

  onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input?.files && input.files.length > 0) {
      const file = input.files[0];
      this.saving = true;
      this.errorMessage = '';
      this.successMessage = '';
      this.practitionerService.uploadPhoto(file).subscribe({
        next: (p) => {
          this.profile = p;
          this.saving = false;
          this.successMessage = this.prefs.translate('PRACTITIONER.PROFILE.SAVE_SUCCESS');
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (e) => {
          this.saving = false;
          this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.PROFILE.SAVE_ERROR');
        }
      });
    }
  }

  save() {
    if (!this.profile) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const payload: PractitionerProfileDTO = {
      ...this.profile,
      consultationFee:
        this.profile.consultationFee == null || Number.isNaN(Number(this.profile.consultationFee))
          ? null
          : Number(this.profile.consultationFee),
    };
    this.practitionerService.update(this.profile.id, payload).subscribe({
      next: (p) => {
        this.profile = p;
        this.saving = false;
        this.successMessage = this.prefs.translate('PRACTITIONER.PROFILE.SAVE_SUCCESS');
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.errorMessage = e.error?.error || this.prefs.translate('PRACTITIONER.PROFILE.SAVE_ERROR');
      },
    });
  }
}
