import { AsyncPipe, NgClass } from '@angular/common';
import { Component, effect, inject, input, output, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { combineLatest, startWith } from 'rxjs';

import type { Appointment, AppointmentType } from '../../models/agenda.model';
import { AgendaStateService } from '../../services/agenda-state.service';

function addMinutes(base: Date, minutes: number): Date {
  return new Date(base.getTime() + minutes * 60_000);
}

function combineDateAndTime(dateStr: string, timeStr: string): Date {
  const [yy, mm, dd] = dateStr.split('-').map((x) => parseInt(x, 10));
  const timeParts = timeStr.split(':');
  const hh = parseInt(timeParts[0] ?? '0', 10);
  const min = parseInt(timeParts[1] ?? '0', 10);
  return new Date(yy, mm - 1, dd, hh, min, 0, 0);
}

function formatHm(d: Date): string {
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/** Début de la minute locale — pour comparer date/heure saisie vs « maintenant » sans rejeter la minute courante. */
function startOfLocalMinute(d: Date): Date {
  const x = new Date(d.getTime());
  x.setSeconds(0, 0);
  return x;
}

function toDateInputValue(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

@Component({
  selector: 'app-appointment-modal',
  standalone: true,
  imports: [AsyncPipe, NgClass, ReactiveFormsModule],
  templateUrl: './appointment-modal.component.html',
  styleUrls: ['./appointment-modal.component.scss'],
})
export class AppointmentModalComponent {
  readonly open = input(false);

  /** Rendez-vous ouvert depuis la grille (édition). `null` = création. */
  readonly appointmentToEdit = input<Appointment | null>(null);

  readonly closed = output<void>();

  private readonly agenda = inject(AgendaStateService);
  private readonly fb = inject(FormBuilder);

  readonly vm$ = combineLatest({
    doctors: this.agenda.doctors$,
    types: this.agenda.appointmentTypes$,
  });

  readonly form = this.fb.group({
    title: ['', Validators.required],
    doctorId: ['', Validators.required],
    typeId: ['', Validators.required],
    startDate: ['', Validators.required],
    startTime: ['', Validators.required],
    durationMinutes: [15, [Validators.required, Validators.min(1)]],
    endTimeDisplay: [''],
    color: ['#93c5fd', Validators.required],
    description: [''],
  });

  constructor() {
    /** Recalcul de l’heure de fin lorsque date / heure début / durée changent. */
    combineLatest([
      this.form.controls.startDate.valueChanges.pipe(startWith(this.form.controls.startDate.value)),
      this.form.controls.startTime.valueChanges.pipe(startWith(this.form.controls.startTime.value)),
      this.form.controls.durationMinutes.valueChanges.pipe(
        startWith(this.form.controls.durationMinutes.value),
      ),
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.syncEndTimeDisplay());

    this.form.controls.startDate.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.appointmentToEdit()) {
        return;
      }
      this.clampStartTimeIfToday();
    });
    this.form.controls.startTime.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.appointmentToEdit()) {
        return;
      }
      this.clampStartTimeIfToday();
    });

    /** Quand le type change : appliquer la durée par défaut & couleur badge en suggestion. */
    this.form.controls.typeId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((id) => {
        const t = this.findTypeById(id);
        if (t) {
          this.form.patchValue(
            { durationMinutes: t.defaultDurationMinutes },
            { emitEvent: true },
          );
        }
      });

    /** Couleur suggérée = colorCode du médecin choisi (planning cohérent). */
    combineLatest([
      this.agenda.doctors$,
      this.form.controls.doctorId.valueChanges.pipe(startWith(this.form.controls.doctorId.value)),
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([docs, id]) => {
        const doc = docs.find((d) => d.id === id);
        if (doc) {
          this.form.patchValue({ color: doc.colorCode }, { emitEvent: false });
        }
      });

    effect(() => {
      const isOpen = this.open();
      const toEdit = this.appointmentToEdit();
      if (!isOpen) {
        return;
      }
      untracked(() => {
        if (toEdit) {
          this.patchFromAppointment(toEdit);
        } else {
          this.resetForm();
        }
      });
    });
  }

  onCancel(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const startDate = v.startDate;
    const startTime = v.startTime;
    const doctorId = v.doctorId;
    const color = v.color;
    const typeId = v.typeId;
    if (!startDate || !startTime || !doctorId || !color || !typeId) {
      return;
    }

    const type = this.findTypeById(typeId);
    if (!type) {
      this.form.controls.typeId.setErrors({ unknown: true });
      return;
    }

    const start = combineDateAndTime(startDate, startTime);
    const duration = Number(v.durationMinutes);
    const end = addMinutes(start, duration);

    const existing = this.appointmentToEdit();
    const now = new Date();
    if (!existing && startOfLocalMinute(start).getTime() < startOfLocalMinute(now).getTime()) {
      window.alert("L'heure de début ne peut pas être dans le passé.");
      return;
    }

    const titleTrim = (v.title ?? '').trim();
    const title = titleTrim || `${type.label} — ${formatHm(start)}`;

    const payload: Omit<Appointment, 'id'> = {
      title,
      typeId: type.id,
      typeCode: type.code,
      typeLabel: type.label,
      typeColor: type.colorCode,
      startTime: start,
      endTime: end,
      durationMinutes: duration,
      description: (v.description ?? '').trim(),
      doctorId,
      color,
    };

    if (existing) {
      if (!titleTrim) {
        this.form.controls.title.markAsTouched();
        return;
      }
      this.agenda.updateAppointment(existing.id, payload).subscribe({ next: () => this.closed.emit() });
    } else {
      this.agenda.createAppointment(payload).subscribe({ next: () => this.closed.emit() });
    }
  }

  markAsCompleted(): void {
    const existing = this.appointmentToEdit();
    if (!existing) return;
    this.agenda.updateAppointmentStatus(existing.id, 'COMPLETED').subscribe({
      next: () => this.closed.emit(),
    });
  }

  markAsNoShow(): void {
    const existing = this.appointmentToEdit();
    if (!existing) return;
    this.agenda.updateAppointmentStatus(existing.id, 'NO_SHOW').subscribe({
      next: () => this.closed.emit(),
    });
  }

  isCustomDurationActive = false;

  isCustomSelected(): boolean {
    const val = this.form.controls.durationMinutes.value;
    if (val === null || val === undefined) {
      return true;
    }
    if (this.isCustomDurationActive) {
      return true;
    }
    return ![15, 20, 30].includes(Number(val));
  }

  selectCustomDuration(): void {
    this.isCustomDurationActive = true;
  }

  setDuration(minutes: number): void {
    this.isCustomDurationActive = false;
    this.form.patchValue({ durationMinutes: minutes });
  }

  /** Résolution robuste : les options du select renvoient toujours une chaîne. */
  private findTypeById(id: string | null | undefined): AppointmentType | undefined {
    const sid = String(id ?? '').trim();
    if (!sid) {
      return undefined;
    }
    return this.agenda.appointmentTypes.find((t) => t.id === sid || t.code === sid);
  }

  private patchFromAppointment(apt: Appointment): void {
    this.isCustomDurationActive = ![15, 20, 30].includes(Number(apt.durationMinutes));
    this.form.patchValue(
      {
        title: apt.title,
        doctorId: apt.doctorId,
        typeId: apt.typeCode || apt.typeId,
        startDate: toDateInputValue(apt.startTime),
        startTime: formatHm(apt.startTime),
        durationMinutes: apt.durationMinutes,
        description: apt.description ?? '',
        color: apt.color,
        endTimeDisplay: '',
      },
      { emitEvent: false },
    );
    this.syncEndTimeDisplay();
  }

  formatTariff(price: number | null | undefined, isVariable?: boolean): string {
    if (isVariable || price == null || Number.isNaN(Number(price))) {
      return 'à discuter';
    }
    return `${Math.round(Number(price)).toLocaleString('fr-FR')} DH`;
  }

  formatTypeOption(type: AppointmentType): string {
    const parts = [type.label];
    if (type.doctorName) {
      parts.push(type.doctorName);
    }
    if (type.price !== undefined) {
      parts.push(this.formatTariff(type.price, type.priceVariable));
    }
    return parts.join(' — ');
  }

  private resetForm(): void {
    const now = new Date();
    now.setSeconds(0, 0);
    const types = this.agenda.appointmentTypes;
    const defaultType = types.find((t) => t.active) ?? types[0];
    const preferredDoctor = this.agenda.selectedDoctorIds[0] ?? '';
    const dateStr = toDateInputValue(now);
    const timeStr = formatHm(now);
    const defaultDuration = defaultType?.defaultDurationMinutes ?? 15;

    this.isCustomDurationActive = ![15, 20, 30].includes(Number(defaultDuration));

    this.form.reset({
      title: defaultType ? `${defaultType.label} — ${timeStr}` : '',
      doctorId: preferredDoctor,
      typeId: defaultType?.id ?? '',
      startDate: dateStr,
      startTime: timeStr,
      durationMinutes: defaultDuration,
      endTimeDisplay: '',
      color: '#93c5fd',
      description: '',
    });
    this.clampStartTimeIfToday();
    this.syncEndTimeDisplay();
  }

  /** Pour la date du jour : borne min de l’input time = heure actuelle (nouveau RDV uniquement). */
  minStartTimeAttr(): string | null {
    if (this.appointmentToEdit()) {
      return null;
    }
    const ds = this.form.controls.startDate.value;
    if (!ds) {
      return null;
    }
    if (ds !== toDateInputValue(new Date())) {
      return null;
    }
    return formatHm(new Date());
  }

  private clampStartTimeIfToday(): void {
    const dateStr = this.form.controls.startDate.value;
    if (!dateStr) {
      return;
    }
    const today = toDateInputValue(new Date());
    if (dateStr !== today) {
      return;
    }
    const minT = formatHm(new Date());
    const cur = (this.form.controls.startTime.value ?? '').trim();
    if (cur && cur < minT) {
      this.form.patchValue({ startTime: minT }, { emitEvent: false });
      this.syncEndTimeDisplay();
    }
  }

  private syncEndTimeDisplay(): void {
    const startDate = this.form.controls.startDate.value;
    const startTime = this.form.controls.startTime.value;
    const durationRaw = this.form.controls.durationMinutes.value;
    const duration = Number(durationRaw);

    if (!startDate || !startTime || !Number.isFinite(duration) || duration < 1) {
      this.form.patchValue({ endTimeDisplay: '—' }, { emitEvent: false });
      return;
    }
    const start = combineDateAndTime(startDate, startTime);
    const end = addMinutes(start, duration);
    this.form.patchValue({ endTimeDisplay: formatHm(end) }, { emitEvent: false });
  }
}
