import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { PractitionerService } from '../../services/practitioner.service';
import { PractitionerProfileDTO, SpecialtyDTO } from '../../models/practitioner.model';
import { PreferencesService } from '../../services/preferences.service';
import { AuthProService } from '../../services/auth-pro.service';

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

  profile: PractitionerProfileDTO | null = null;
  specialties: SpecialtyDTO[] = [];
  saving = false;
  errorMessage = '';
  successMessage = '';

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
                this.prefs.translate('profile.notFound');
              return of<ProfileBundle>({
                profile: null,
                specialties: [],
                loadError: typeof msg === 'string' ? msg : this.prefs.translate('profile.notFound'),
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
      });
  }

  hasSpecialty(id: number): boolean {
    return !!this.profile?.specialites?.some((s) => s.id === id);
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

  save() {
    if (!this.profile) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.practitionerService.update(this.profile.id, this.profile).subscribe({
      next: (p) => {
        this.profile = p;
        this.saving = false;
        this.successMessage = this.prefs.translate('profile.saveSuccess');
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (e) => {
        this.saving = false;
        this.errorMessage = e.error?.error || this.prefs.translate('profile.saveError');
      },
    });
  }
}
