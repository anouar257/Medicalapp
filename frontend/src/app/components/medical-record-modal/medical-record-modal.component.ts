import { Component, inject, input, output, signal, effect, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { take } from 'rxjs';
import type { Appointment } from '../../models/agenda.model';
import { MedicalRecordService, MedicalRecordDTO } from '../../services/medical-record.service';
import { AgendaStateService } from '../../services/agenda-state.service';

@Component({
  selector: 'app-medical-record-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './medical-record-modal.component.html',
  styleUrls: ['./medical-record-modal.component.scss']
})
export class MedicalRecordModalComponent {
  readonly open = input(false);
  readonly appointment = input<Appointment | null>(null);
  readonly closed = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly medicalRecordService = inject(MedicalRecordService);
  private readonly agenda = inject(AgendaStateService);

  protected readonly isLoading = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  private isNewRecord = false;

  readonly form = this.fb.group({
    antecedents: [''],
    symptomes: [''],
    consultation: [''],
    diagnostique: [''],
    consommable: [''],
    acte: [''],
    radiologie: ['']
  });

  constructor() {
    effect(() => {
      const isOpen = this.open();
      const apt = this.appointment();
      if (!isOpen || !apt) {
        return;
      }
      untracked(() => {
        this.loadMedicalRecord(Number(apt.id));
      });
    });
  }

  getDoctorName(): string {
    const apt = this.appointment();
    if (!apt) return '';
    const doc = this.agenda.doctors.find(d => String(d.id) === String(apt.doctorId));
    return doc ? doc.name : 'Médecin inconnu';
  }

  private loadMedicalRecord(appointmentId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.medicalRecordService.getMedicalRecord(appointmentId)
      .pipe(take(1))
      .subscribe({
        next: (record) => {
          this.form.patchValue({
            antecedents: record.antecedents ?? '',
            symptomes: record.symptomes ?? '',
            consultation: record.consultation ?? '',
            diagnostique: record.diagnostique ?? '',
            consommable: record.consommable ?? '',
            acte: record.acte ?? '',
            radiologie: record.radiologie ?? ''
          });
          this.isLoading.set(false);
          this.isNewRecord = false;
        },
        error: (err) => {
          this.isLoading.set(false);
          if (err.status === 404) {
            this.form.reset({
              antecedents: '',
              symptomes: '',
              consultation: '',
              diagnostique: '',
              consommable: '',
              acte: '',
              radiologie: ''
            });
            this.isNewRecord = true;
          } else {
            this.errorMessage.set("Erreur lors du chargement du dossier médical.");
          }
        }
      });
  }

  onCancel(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    const apt = this.appointment();
    if (!apt) {
      return;
    }
    const appointmentId = Number(apt.id);
    this.isSaving.set(true);
    this.errorMessage.set(null);

    const recordValue = this.form.value;
    const dto: MedicalRecordDTO = {
      appointmentId,
      antecedents: recordValue.antecedents ?? '',
      symptomes: recordValue.symptomes ?? '',
      consultation: recordValue.consultation ?? '',
      diagnostique: recordValue.diagnostique ?? '',
      consommable: recordValue.consommable ?? '',
      acte: recordValue.acte ?? '',
      radiologie: recordValue.radiologie ?? ''
    };

    const action$ = this.isNewRecord
      ? this.medicalRecordService.createMedicalRecord(appointmentId, dto)
      : this.medicalRecordService.updateMedicalRecord(appointmentId, dto);

    action$.pipe(take(1)).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closed.emit();
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set("Erreur lors de la sauvegarde du dossier médical.");
      }
    });
  }
}
