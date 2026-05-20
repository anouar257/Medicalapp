import { AsyncPipe } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

import type { Doctor } from '../../../models/agenda.model';
import { AgendaStateService, extractHttpErrorDetail } from '../../../services/agenda-state.service';
import { PreferencesService } from '../../../services/preferences.service';
import { ThemeService } from '../../../services/theme.service';
import { resolveDoctorPhotoUrl } from '../../../utils/media-url';

/** Gestion des médecins synchronisés avec l’agenda (praticien). */
@Component({
  selector: 'app-cabinet-doctors',
  standalone: true,
  imports: [AsyncPipe, RouterLink],
  templateUrl: './cabinet-doctors.component.html',
  styleUrls: ['./cabinet-doctors.component.scss'],
})
export class CabinetDoctorsComponent {
  private readonly agenda = inject(AgendaStateService);
  readonly theme = inject(ThemeService);
  readonly prefs = inject(PreferencesService);

  private readonly photoFileInput = viewChild<ElementRef<HTMLInputElement>>('photoFile');

  protected readonly resolveDoctorPhotoUrl = resolveDoctorPhotoUrl;

  readonly predefinedColors = [
    '#ef4444',
    '#f97316',
    '#f59e0b',
    '#84cc16',
    '#10b981',
    '#06b6d4',
    '#3b82f6',
    '#8b5cf6',
    '#d946ef',
    '#f43f5e',
    '#64748b',
  ];

  readonly pendingFile = signal<File | null>(null);

  readonly doctors$ = this.agenda.doctors$;

  readonly formName = signal('');
  readonly formSpecialty = signal('');
  readonly formColor = signal('');
  readonly formPhoto = signal('');
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly deleteFeedback = signal<string | null>(null);

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');
  }

  startEdit(d: Doctor): void {
    this.formError.set(null);
    this.deleteFeedback.set(null);
    this.editingId.set(d.id);
    this.formName.set(d.name);
    this.formSpecialty.set(d.specialty ?? '');
    this.formColor.set(d.colorCode ?? '');
    this.formPhoto.set(d.photoUrl ?? '');
    this.pendingFile.set(null);
    this.clearPhotoFileInput();
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.formName.set('');
    this.formSpecialty.set('');
    this.formColor.set('');
    this.formPhoto.set('');
    this.pendingFile.set(null);
    this.clearPhotoFileInput();
    this.formError.set(null);
    this.deleteFeedback.set(null);
  }

  onPhotoFileSelected(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.pendingFile.set(file);
  }

  private clearPhotoFileInput(): void {
    const el = this.photoFileInput()?.nativeElement;
    if (el) {
      el.value = '';
    }
  }

  submit(): void {
    const name = this.formName().trim();
    if (!name) {
      return;
    }
    this.formError.set(null);
    this.deleteFeedback.set(null);
    this.saving.set(true);
    const colorRaw = this.formColor().trim();
    const photoRaw = this.formPhoto().trim();
    const specRaw = this.formSpecialty().trim();
    const pending = this.pendingFile();
    const body = {
      name,
      ...(colorRaw ? { colorCode: colorRaw } : {}),
      ...(photoRaw && !pending ? { photoUrl: photoRaw } : {}),
      ...(specRaw ? { specialty: specRaw } : {}),
    };
    const id = this.editingId();
    const doneSaving = (): void => this.saving.set(false);

    const afterDoctorSaved = (doctorId: string): void => {
      const file = this.pendingFile();
      if (file) {
        this.agenda.uploadDoctorPhoto(doctorId, file).subscribe({
          next: () => {
            this.pendingFile.set(null);
            this.clearPhotoFileInput();
            this.cancelEdit();
          },
          complete: () => doneSaving(),
          error: (err: unknown) => {
            doneSaving();
            this.formError.set(extractHttpErrorDetail(err) ?? 'Échec du téléversement de la photo.');
          },
        });
        return;
      }
      this.cancelEdit();
      doneSaving();
    };

    if (id) {
      this.agenda.updateDoctor(id, body).subscribe({
        next: () => afterDoctorSaved(id),
        error: (err: unknown) => {
          doneSaving();
          this.formError.set(extractHttpErrorDetail(err) ?? 'Impossible d’enregistrer le médecin.');
        },
      });
      return;
    }
    this.agenda.createDoctor(body).subscribe({
      next: (doc) => afterDoctorSaved(doc.id),
      error: (err: unknown) => {
        doneSaving();
        this.formError.set(extractHttpErrorDetail(err) ?? 'Impossible de créer le médecin.');
      },
    });
  }

  remove(d: Doctor): void {
    if ((d.appointmentCount ?? 0) > 0) {
      this.deleteFeedback.set(
        `« ${d.name} » a encore ${d.appointmentCount} rendez-vous en base : supprimez ou modifiez ces RDV depuis l’agenda avant de supprimer le médecin.`,
      );
      return;
    }
    if (!window.confirm(`Supprimer « ${d.name} » ?`)) {
      return;
    }
    this.deleteFeedback.set(null);
    this.deletingId.set(d.id);
    this.agenda.deleteDoctor(d.id).subscribe({
      next: () => this.deletingId.set(null),
      error: (err: unknown) => {
        this.deletingId.set(null);
        const msg = conflictMessage(err);
        if (msg) {
          this.deleteFeedback.set(msg);
        }
      },
    });
  }
}

function conflictMessage(err: unknown): string | null {
  if (!err || typeof err !== 'object') {
    return null;
  }
  const e = err as { status?: number; error?: unknown };
  if (e.status !== 409) {
    return null;
  }
  const ex = e.error;
  if (typeof ex === 'string' && ex.trim()) {
    return ex.trim();
  }
  if (ex && typeof ex === 'object' && 'message' in ex) {
    const m = (ex as { message?: unknown }).message;
    if (typeof m === 'string' && m.trim()) {
      return m.trim();
    }
  }
  return 'Impossible de supprimer : des rendez-vous sont encore associés à ce médecin.';
}
