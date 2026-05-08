import { AsyncPipe } from '@angular/common';
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
  imports: [AsyncPipe, ReactiveFormsModule],
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="appointment-modal-title"
      >
        <button
          type="button"
          class="absolute inset-0 bg-slate-950/55 backdrop-blur-sm transition hover:bg-slate-950/65 dark:bg-black/60 dark:hover:bg-black/70"
          aria-label="Fermer"
          (click)="onCancel()"
        ></button>

        <div
          class="relative z-10 flex w-full max-w-4xl flex-col overflow-hidden rounded-[1.75rem] border border-slate-200/80 bg-white shadow-[0_24px_80px_-32px_rgba(15,23,42,0.55)] ring-1 ring-slate-100 dark:border-slate-600/70 dark:bg-[#1E293B] dark:ring-slate-700/80"
          (click)="$event.stopPropagation()"
        >
          <div class="shrink-0 border-b border-slate-100 px-6 py-5 dark:border-slate-700">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="appointment-modal-title"
                  class="text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-100"
                >
                  {{ appointmentToEdit() ? 'Modifier le rendez-vous' : 'Nouveau rendez-vous' }}
                </h2>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  Type, intitulé et description à gauche · médecin, date et durée à droite.
                </p>
              </div>
              <button
                type="button"
                class="flex size-10 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-base text-slate-500 transition hover:border-slate-300 hover:bg-white hover:text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-400/70 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
                (click)="onCancel()"
              >
                ✕
              </button>
            </div>
          </div>

          @if (vm$ | async; as vm) {
            <form class="flex flex-col px-6 py-5" [formGroup]="form" (ngSubmit)="onSubmit()">
              <div class="grid gap-6 md:grid-cols-2">
                <!-- Colonne gauche : type → intitulé → description -->
                <div class="space-y-4">
                  <div class="grid gap-1.5">
                    <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-type">Type</label>
                    <select
                      id="apt-type"
                      formControlName="typeId"
                      class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                    >
                      <option value="" disabled>Choisir un type…</option>
                      @for (t of vm.types; track t.id) {
                        <option [value]="t.id">{{ t.label }}</option>
                      }
                    </select>
                  </div>

                  <div class="grid gap-1.5">
                    <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-title">Intitulé</label>
                    <input
                      id="apt-title"
                      type="text"
                      formControlName="title"
                      autocomplete="off"
                      class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100 dark:placeholder:text-slate-500"
                      placeholder="Ex. Consultation ou suivi — préciser le motif si besoin"
                    />
                  </div>

                  <div class="grid gap-1.5">
                    <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-desc">Description</label>
                    <textarea
                      id="apt-desc"
                      formControlName="description"
                      rows="6"
                      class="w-full resize-none rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                      placeholder="Notes cliniques, motif…"
                    ></textarea>
                  </div>
                </div>

                <!-- Colonne droite : médecin, date, heure, durée, fin, couleur -->
                <div class="space-y-4">
                  <div class="grid gap-1.5">
                    <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-doctor">Médecin</label>
                    <select
                      id="apt-doctor"
                      formControlName="doctorId"
                      class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                    >
                      <option value="" disabled>Choisir un médecin</option>
                      @for (doc of vm.doctors; track doc.id) {
                        <option [value]="doc.id">{{ doc.name }}</option>
                      }
                    </select>
                  </div>

                  <div class="grid gap-3 sm:grid-cols-2">
                    <div class="grid gap-1.5">
                      <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-start-date">Date</label>
                      <input
                        id="apt-start-date"
                        type="date"
                        formControlName="startDate"
                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                      />
                    </div>
                    <div class="grid gap-1.5">
                      <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-start-time">Début</label>
                      <input
                        id="apt-start-time"
                        type="time"
                        formControlName="startTime"
                        [attr.min]="minStartTimeAttr()"
                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                      />
                    </div>
                  </div>

                  <div class="grid gap-3 sm:grid-cols-2">
                    <div class="grid gap-1.5">
                      <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-duration">Durée (min)</label>
                      <input
                        id="apt-duration"
                        type="number"
                        min="1"
                        step="1"
                        formControlName="durationMinutes"
                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition hover:border-slate-300 focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-400/40 dark:border-slate-600 dark:bg-slate-800/90 dark:text-slate-100"
                      />
                    </div>
                    <div class="grid gap-1.5">
                      <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-end-time">Fin</label>
                      <input
                        id="apt-end-time"
                        type="text"
                        formControlName="endTimeDisplay"
                        readonly
                        tabindex="-1"
                        class="w-full cursor-not-allowed rounded-xl border border-dashed border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 dark:border-slate-600 dark:bg-slate-900/50 dark:text-slate-300"
                      />
                    </div>
                  </div>

                  <div class="grid gap-1.5">
                    <label class="text-sm font-medium text-slate-800 dark:text-slate-200" for="apt-color">Couleur</label>
                    <div class="flex items-center gap-3">
                      <input
                        id="apt-color"
                        type="color"
                        formControlName="color"
                        class="h-11 w-14 cursor-pointer rounded-lg border border-slate-200 bg-white p-1 shadow-sm dark:border-slate-600"
                      />
                      <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{
                        form.controls.color.value
                      }}</span>
                    </div>
                  </div>
                </div>
              </div>

              @if (form.touched && form.invalid) {
                <p class="mt-4 text-sm text-red-600 dark:text-red-400">
                  Veuillez compléter les champs obligatoires.
                </p>
              }

              <div
                class="mt-6 flex shrink-0 flex-wrap justify-end gap-3 border-t border-slate-100 pt-5 dark:border-slate-700"
              >
                <button
                  type="button"
                  class="rounded-full border border-slate-200 bg-white px-6 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-300 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                  (click)="onCancel()"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  class="rounded-full bg-blue-600 px-8 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-600/25 transition hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-offset-2 focus:ring-offset-white dark:focus:ring-offset-[#1E293B] disabled:cursor-not-allowed disabled:opacity-50"
                  [disabled]="form.invalid"
                >
                  {{ appointmentToEdit() ? 'Mettre à jour' : 'Créer' }}
                </button>
              </div>
            </form>
          }
        </div>
      </div>
    }
  `,
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

  /** Résolution robuste : les options du select renvoient toujours une chaîne. */
  private findTypeById(id: string | null | undefined): AppointmentType | undefined {
    const sid = String(id ?? '').trim();
    if (!sid) {
      return undefined;
    }
    return this.agenda.appointmentTypes.find((t) => t.id === sid);
  }

  private patchFromAppointment(apt: Appointment): void {
    this.form.patchValue(
      {
        title: apt.title,
        doctorId: apt.doctorId,
        typeId: apt.typeId,
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

  private resetForm(): void {
    const now = new Date();
    now.setSeconds(0, 0);
    const types = this.agenda.appointmentTypes;
    const defaultType = types.find((t) => t.active) ?? types[0];
    const preferredDoctor = this.agenda.selectedDoctorIds[0] ?? '';
    const dateStr = toDateInputValue(now);
    const timeStr = formatHm(now);

    this.form.reset({
      title: defaultType ? `${defaultType.label} — ${timeStr}` : '',
      doctorId: preferredDoctor,
      typeId: defaultType?.id ?? '',
      startDate: dateStr,
      startTime: timeStr,
      durationMinutes: defaultType?.defaultDurationMinutes ?? 15,
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
