import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { AgendaService, AppointmentPatientDTO } from '../../services/agenda.service';
import { MessagingService } from '../../services/messaging.service';
import { ProcheService } from '../../services/proche.service';
import { PreferencesService } from '../../services/preferences.service';
import { AuthResponse } from '../../models/patient.model';
import { Proche } from '../../models/proche.model';
import type { MessageResponseDto, MessagingSubject } from '../../models/messaging.model';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import { formatHttpError } from '../../utils/http-error-message';

@Component({
  selector: 'app-patient-messages',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DatePipe,
    AppPreferencesToolbarComponent,
  ],
  templateUrl: './patient-messages.component.html',
  styleUrls: ['./patient-messages.component.scss'],
})
export class PatientMessagesComponent implements OnInit {
  readonly prefs = inject(PreferencesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly agenda = inject(AgendaService);
  private readonly messaging = inject(MessagingService);
  private readonly procheService = inject(ProcheService);
  private readonly destroyRef = inject(DestroyRef);

  patient: AuthResponse | null = null;
  proches: Proche[] = [];
  appointments: AppointmentPatientDTO[] = [];
  messages: MessageResponseDto[] = [];
  practitionerLabelByExternalId = new Map<number, string>();

  view: 'history' | 'compose' = 'history';
  wizardStep = 1;
  concernTargetType: 'SELF' | 'PROCHE' | null = null;
  concernTargetId: number | null = null;
  selectedPractitionerExternalId: number | null = null;
  selectedSubject: MessagingSubject | null = null;
  customSubject = '';
  messageContent = '';
  selectedFiles: File[] = [];

  loading = true;
  /** Avertissements non bloquants (un ou plusieurs appels API ont échoué). */
  loadWarnings: string[] = [];
  sending = false;
  sendError = '';

  readonly subjectKeys: MessagingSubject[] = [
    'RESULTAT_EXAMEN',
    'QUESTION_AVANT_RDV',
    'QUESTION_APRES_RDV',
    'AUTRE',
  ];

  ngOnInit(): void {
    const p = this.auth.getCurrentPatient();
    if (!p) {
      void this.router.navigate(['/auth/login']);
      return;
    }
    this.patient = p;
    this.loadWarnings = [];
    forkJoin({
      proches: this.procheService.getMyProches().pipe(
        catchError((err) => {
          this.loadWarnings.push(
            formatHttpError(err, this.prefs.translate('messaging.loadWarningProches')),
          );
          return of([] as Proche[]);
        }),
      ),
      appts: this.agenda.getAppointmentsForPatient(p.patientId).pipe(
        catchError((err) => {
          this.loadWarnings.push(
            formatHttpError(err, this.prefs.translate('messaging.loadWarningAgenda')),
          );
          return of([] as AppointmentPatientDTO[]);
        }),
      ),
      msgs: this.messaging.getPatientMessages(p.patientId).pipe(
        catchError((err) => {
          this.loadWarnings.push(
            formatHttpError(err, this.prefs.translate('messaging.loadWarningMessaging')),
          );
          return of([] as MessageResponseDto[]);
        }),
      ),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ proches, appts, msgs }) => {
          this.proches = proches;
          this.appointments = appts;
          this.messages = [...msgs].sort(
            (a, b) => new Date(b.sentAt).getTime() - new Date(a.sentAt).getTime(),
          );
          this.rebuildPractitionerLabels();
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.loadWarnings.push(this.prefs.translate('common.serviceUnavailable'));
        },
      });
  }

  trackProche(_index: number, pr: Proche): string | number {
    return pr.id ?? `p-${pr.email}-${_index}`;
  }

  get consultablePractitioners(): { externalId: number; name: string; specialty: string }[] {
    const map = new Map<number, { externalId: number; name: string; specialty: string }>();
    for (const a of this.appointments) {
      if (a.status === 'CANCELLED') continue;
      const ext = a.doctorExternalPractitionerId;
      if (ext == null) continue;
      if (!map.has(ext)) {
        map.set(ext, {
          externalId: ext,
          name: a.doctorName,
          specialty: a.doctorSpecialty,
        });
      }
    }
    return [...map.values()].sort((x, y) => x.name.localeCompare(y.name));
  }

  pickMe(): void {
    if (!this.patient) return;
    this.concernTargetType = 'SELF';
    this.concernTargetId = this.patient.patientId;
  }

  pickProche(p: Proche): void {
    if (p.id != null) {
      this.concernTargetType = 'PROCHE';
      this.concernTargetId = p.id;
    }
  }

  pickPractitioner(extId: number): void {
    this.selectedPractitionerExternalId = extId;
  }

  pickSubject(s: MessagingSubject): void {
    this.selectedSubject = s;
  }

  subjectLabel(s: MessagingSubject): string {
    return this.prefs.translate(`messaging.subject.${s}`);
  }

  doctorNameFor(m: MessageResponseDto): string {
    const fromBackend =
      this.patient?.patientId != null && m.senderPatientId === this.patient.patientId
        ? m.receiverName?.trim()
        : m.senderName?.trim();
    if (fromBackend) return fromBackend;
    const ext = m.senderPractitionerProfileId ?? m.receiverPractitionerProfileId;
    if (ext == null) return this.prefs.translate('messaging.unknownDoctor');
    return this.practitionerLabelByExternalId.get(ext) ?? this.prefs.translate('messaging.unknownDoctor');
  }

  flowLabel(m: MessageResponseDto): string {
    const me = this.patient?.patientId;
    if (me != null && m.senderPatientId === me) {
      return this.prefs.translate('messaging.flow.sent');
    }
    return this.prefs.translate('messaging.flow.received');
  }

  concernedLabel(m: MessageResponseDto): string {
    const api = m.concernedPersonName?.trim();
    if (api) return api;
    const me = this.patient?.patientId;
    if (me != null && m.concernedPersonId === me) {
      return this.prefs.translate('messaging.concerned.me');
    }
    const pr = this.proches.find((x) => x.id === m.concernedPersonId);
    if (pr) return `${pr.prenom} ${pr.nom}`;
    return this.prefs.translate('messaging.concerned.relative');
  }

  canGoStep2(): boolean {
    return this.concernTargetId != null;
  }

  canGoStep3(): boolean {
    return this.selectedPractitionerExternalId != null;
  }

  canGoStep4(): boolean {
    if (this.selectedSubject === 'AUTRE') {
      return !!this.customSubject.trim();
    }
    return this.selectedSubject != null;
  }

  canAdvanceFromCurrentStep(): boolean {
    switch (this.wizardStep) {
      case 1:
        return this.canGoStep2();
      case 2:
        return this.canGoStep3();
      case 3:
        return this.canGoStep4();
      default:
        return false;
    }
  }

  canSubmit(): boolean {
    const subjectOk =
      this.selectedSubject === 'AUTRE'
        ? !!this.customSubject.trim()
        : !!this.selectedSubject;
    return (
      !!this.messageContent.trim() &&
      this.concernTargetId != null &&
      this.selectedPractitionerExternalId != null &&
      subjectOk &&
      !this.sending
    );
  }

  nextStep(): void {
    if (!this.canAdvanceFromCurrentStep() || this.wizardStep >= 4) return;
    this.wizardStep++;
  }

  prevStep(): void {
    if (this.wizardStep > 1) this.wizardStep--;
  }

  onFiles(ev: Event): void {
    const el = ev.target as HTMLInputElement;
    this.selectedFiles = el.files?.length ? Array.from(el.files) : [];
  }

  removeFile(i: number): void {
    this.selectedFiles = this.selectedFiles.filter((_, idx) => idx !== i);
  }

  submit(): void {
    if (!this.patient || !this.canSubmit()) return;
    this.sendError = '';
    this.sending = true;
    const finalContent =
      this.selectedSubject === 'AUTRE'
        ? `[${this.customSubject.trim()}]\n\n${this.messageContent.trim()}`
        : this.messageContent.trim();

    this.messaging
      .sendMessageWithFiles(
        {
          direction: 'PATIENT_TO_PRACTITIONER',
          concernedPersonId: this.concernTargetId!,
          receiverPractitionerProfileId: this.selectedPractitionerExternalId!,
          subject: this.selectedSubject!,
          content: finalContent,
        },
        this.selectedFiles,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (msg) => {
          this.sending = false;
          this.messages = [msg, ...this.messages];
          this.resetWizard();
          this.view = 'history';
        },
        error: (e) => {
          this.sending = false;
          this.sendError = formatHttpError(e, this.prefs.translate('common.serviceUnavailable'));
        },
      });
  }

  onOpenMessage(m: MessageResponseDto): void {
    const me = this.patient?.patientId;
    if (me == null || m.read || m.receiverPatientId !== me) return;
    this.messaging
      .markAsRead(m.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (u) => Object.assign(m, u),
      });
  }

  resetWizard(): void {
    this.wizardStep = 1;
    this.concernTargetType = null;
    this.concernTargetId = null;
    this.selectedPractitionerExternalId = null;
    this.selectedSubject = null;
    this.customSubject = '';
    this.messageContent = '';
    this.selectedFiles = [];
    this.sendError = '';
  }

  /** Résumé du nombre de fichiers (i18n). */
  filesCountLabel(): string {
    const n = this.selectedFiles.length;
    if (n <= 0) return '';
    if (n === 1) return this.prefs.translate('messaging.files.one');
    return this.prefs.translate('messaging.files.many').replace('{{n}}', String(n));
  }

  openCompose(): void {
    this.resetWizard();
    this.view = 'compose';
  }

  private rebuildPractitionerLabels(): void {
    this.practitionerLabelByExternalId.clear();
    for (const a of this.appointments) {
      const ext = a.doctorExternalPractitionerId;
      if (ext != null && !this.practitionerLabelByExternalId.has(ext)) {
        this.practitionerLabelByExternalId.set(ext, a.doctorName);
      }
    }
  }
}
