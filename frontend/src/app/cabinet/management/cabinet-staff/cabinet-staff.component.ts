import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { distinctUntilChanged, take } from 'rxjs';
import { ProUserDTO } from '../../../models/practitioner.model';
import { AuthProService } from '../../../services/auth-pro.service';
import { PreferencesService } from '../../../services/preferences.service';
import { PractitionerService } from '../../../services/practitioner.service';

/** Gestion des assistants du cabinet (praticien). */
@Component({
  selector: 'app-cabinet-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cabinet-staff.component.html',
})
export class CabinetStaffComponent {
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly authPro = inject(AuthProService);
  private readonly destroyRef = inject(DestroyRef);

  assistants: ProUserDTO[] = [];
  loading = true;
  listError = '';

  showCreate = false;
  creating = false;
  createError = '';

  form = {
    nom: '',
    prenom: '',
    email: '',
    motDePasse: '',
  };

  successMessage = '';

  constructor() {
    toObservable(this.authPro.organizationId)
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh());
  }

  get canManageAssistants(): boolean {
    return this.authPro.getRole() === 'PRATICIEN';
  }

  refresh(): void {
    this.loading = true;
    this.listError = '';
    this.practitionerService
      .listMyCabinetAssistants()
      .pipe(take(1))
      .subscribe({
        next: (list) => {
          this.assistants = list ?? [];
          this.loading = false;
        },
        error: () => {
          this.listError = this.prefs.translate('cabinet.users.loadError');
          this.assistants = [];
          this.loading = false;
        },
      });
  }

  openCreate(): void {
    this.showCreate = true;
    this.createError = '';
    this.form = { nom: '', prenom: '', email: '', motDePasse: '' };
  }

  closeCreate(): void {
    this.showCreate = false;
  }

  submitCreate(): void {
    if (this.form.motDePasse.length < 8) {
      this.createError = this.prefs.translate('cabinet.users.passwordHint');
      return;
    }
    this.creating = true;
    this.createError = '';
    this.practitionerService.createAssistant({ ...this.form }).subscribe({
      next: () => {
        this.creating = false;
        this.successMessage = this.prefs.translate('cabinet.users.success');
        this.showCreate = false;
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 5000);
      },
      error: (e) => {
        this.creating = false;
        const msg = e.error?.message ?? e.error?.error ?? e.message;
        this.createError =
          typeof msg === 'string' && msg.length > 0
            ? msg
            : this.prefs.translate('cabinet.staff.createFailedGeneric');
      },
    });
  }

  deactivate(u: ProUserDTO): void {
    if (!this.canManageAssistants) return;
    if (!confirm(this.prefs.translate('cabinet.users.deactivateConfirm'))) return;
    this.practitionerService.desactiverUser(u.id).subscribe({
      next: () => {
        this.successMessage = this.prefs.translate('cabinet.users.statusInactive');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: () => {
        this.listError = this.prefs.translate('cabinet.users.loadError');
      },
    });
  }

  reactivate(u: ProUserDTO): void {
    if (!this.canManageAssistants) return;
    this.practitionerService.reactiverUser(u.id).subscribe({
      next: () => {
        this.successMessage = this.prefs.translate('cabinet.users.statusActive');
        this.refresh();
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: () => {
        this.listError = this.prefs.translate('cabinet.users.loadError');
      },
    });
  }
}
