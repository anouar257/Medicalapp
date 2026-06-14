import { Component, DestroyRef, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
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
import type { MessageResponseDto, MessagingSubject, ChatConversation, AttachmentPayload } from '../../models/messaging.model';
import { formatHttpError } from '../../utils/http-error-message';

@Component({
  selector: 'app-patient-messages',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DatePipe,
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

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;
  @ViewChild('fileInput') private fileInput!: ElementRef;

  patient: AuthResponse | null = null;
  proches: Proche[] = [];
  appointments: AppointmentPatientDTO[] = [];
  practitionerLabelByExternalId = new Map<number, string>();

  // Variables pour la vue Chat WhatsApp
  conversations: ChatConversation[] = [];
  activeConversation: ChatConversation | null = null;
  replyText = '';
  selectedFile: File | null = null;
  selectedFileBase64: string | null = null;

  view: 'chat' | 'compose' = 'chat';
  wizardStep = 1;
  concernTargetType: 'SELF' | 'PROCHE' | null = null;
  concernTargetId: number | null = null;
  selectedPractitionerExternalId: number | null = null;
  selectedSubject: MessagingSubject | null = null;
  customSubject = '';
  messageContent = '';
  
  // Fichiers sélectionnés pour le mode Compose
  composeFiles: {file: File, base64: string}[] = [];

  loading = true;
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
          this.loadWarnings.push(formatHttpError(err, this.prefs.translate('PATIENT.MESSAGES.LOAD_WARNING_PROCHES')));
          return of([] as Proche[]);
        }),
      ),
      appts: this.agenda.getAppointmentsForPatient(p.patientId).pipe(
        catchError((err) => {
          this.loadWarnings.push(formatHttpError(err, this.prefs.translate('PATIENT.MESSAGES.LOAD_WARNING_AGENDA')));
          return of([] as AppointmentPatientDTO[]);
        }),
      ),
      msgs: this.messaging.getPatientMessages(p.patientId).pipe(
        catchError((err) => {
          this.loadWarnings.push(formatHttpError(err, this.prefs.translate('PATIENT.MESSAGES.LOAD_WARNING_MESSAGING')));
          return of([] as MessageResponseDto[]);
        }),
      ),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ proches, appts, msgs }) => {
          this.proches = proches;
          this.appointments = appts;
          this.rebuildPractitionerLabels();
          this.groupMessagesIntoConversations(msgs);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.loadWarnings.push(this.prefs.translate('COMMON.SERVICE_UNAVAILABLE'));
        },
      });
  }

  // --- Regroupement en ChatConversation ---

  private groupMessagesIntoConversations(list: MessageResponseDto[]): void {
    const sorted = [...list].sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime());
    const convMap = new Map<string, ChatConversation>();

    for (const m of sorted) {
      // Pour le patient, l'interlocuteur est le praticien
      const contactId = m.senderPractitionerProfileId ?? m.receiverPractitionerProfileId;
      if (!contactId) continue;

      const convId = `practitioner_${contactId}_concerned_${m.concernedPersonId}`;

      if (!convMap.has(convId)) {
        convMap.set(convId, {
          id: convId,
          contactId: contactId,
          concernedPersonId: m.concernedPersonId,
          contactName: this.getPractitionerName(contactId),
          messages: [],
          lastMessage: m,
          unreadCount: 0
        });
      }

      const conv = convMap.get(convId)!;
      conv.messages.push(m);
      conv.lastMessage = m;

      // Compter les non-lus (si reçu par le patient)
      if (!m.read && m.receiverPatientId === this.patient?.patientId) {
        conv.unreadCount++;
      }
    }

    this.conversations = Array.from(convMap.values()).sort(
      (a, b) => new Date(b.lastMessage.sentAt).getTime() - new Date(a.lastMessage.sentAt).getTime()
    );

    if (this.activeConversation) {
      const updated = this.conversations.find(c => c.id === this.activeConversation!.id);
      if (updated) {
        this.activeConversation = updated;
        this.scrollToBottom();
      }
    }
  }

  private getPractitionerName(extId: number): string {
    return this.practitionerLabelByExternalId.get(extId) || `${this.prefs.translate('PATIENT.MESSAGES.UNKNOWN_DOCTOR')} #${extId}`;
  }

  selectConversation(c: ChatConversation): void {
    this.activeConversation = c;
    this.replyText = '';
    this.sendError = '';
    this.clearFile();

    // Marquer les messages comme lus
    let hasUnread = false;
    for (const m of c.messages) {
      if (!m.read && m.receiverPatientId === this.patient?.patientId) {
        m.read = true;
        hasUnread = true;
        this.messaging.markAsRead(m.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
      }
    }
    
    if (hasUnread) {
      c.unreadCount = 0;
    }
    
    setTimeout(() => this.scrollToBottom(), 50);
  }

  private scrollToBottom(): void {
    try {
      if (this.myScrollContainer) {
        this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
      }
    } catch (err) {}
  }

  // --- Pièces jointes (Reply depuis le Chat) ---

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const dataUrl = e.target.result;
        const base64Index = dataUrl.indexOf('base64,') + 7;
        this.selectedFileBase64 = dataUrl.substring(base64Index);
      };
      reader.readAsDataURL(file);
    }
    event.target.value = '';
  }

  clearFile(): void {
    this.selectedFile = null;
    this.selectedFileBase64 = null;
  }

  sendReply(): void {
    if (!this.activeConversation || !this.patient) return;
    if (!this.replyText.trim() && !this.selectedFile) return;

    this.sending = true;
    this.sendError = '';

    const attachments: AttachmentPayload[] = [];
    if (this.selectedFile && this.selectedFileBase64) {
      attachments.push({
        fileName: this.selectedFile.name,
        fileType: this.selectedFile.type || 'application/octet-stream',
        base64Data: this.selectedFileBase64
      });
    }

    this.messaging
      .sendMessage({
        direction: 'PATIENT_TO_PRACTITIONER',
        receiverPractitionerProfileId: this.activeConversation.contactId,
        concernedPersonId: this.activeConversation.concernedPersonId,
        subject: 'QUESTION_APRES_RDV', 
        content: this.replyText.trim() || ' ',
        attachments: attachments,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (msg) => {
          this.sending = false;
          this.activeConversation!.messages.push(msg);
          this.activeConversation!.lastMessage = msg;
          this.conversations = this.conversations.sort(
            (a, b) => new Date(b.lastMessage.sentAt).getTime() - new Date(a.lastMessage.sentAt).getTime()
          );
          
          this.replyText = '';
          this.clearFile();
          setTimeout(() => this.scrollToBottom(), 50);
        },
        error: (e) => {
          this.sending = false;
          this.sendError = formatHttpError(e, this.prefs.translate('COMMON.SERVICE_UNAVAILABLE'));
        },
      });
  }

  download(m: MessageResponseDto, attId: number, fileName: string): void {
    this.messaging
      .downloadAttachment(m.id, attId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = fileName;
          a.click();
          URL.revokeObjectURL(url);
        },
      });
  }

  // --- Wizard Compose (Nouveau Message) ---

  trackProche(_index: number, pr: Proche): string | number {
    return pr.id ?? `p-${pr.email}-${_index}`;
  }

  get consultablePractitioners(): { externalId: number; name: string; specialty: string }[] {
    const map = new Map<number, { externalId: number; name: string; specialty: string }>();
    for (const a of this.appointments) {
      if (a.status !== 'COMPLETED') continue;
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

  checkLimitsForPractitionerAndConcerned(practitionerId: number | null, concernedPersonId: number | null): { blocked: boolean; reason: string } {
    if (practitionerId == null || concernedPersonId == null) {
      return { blocked: false, reason: '' };
    }

    // 1. Check completed appointment first
    const hasCompleted = this.appointments.some(a => 
      a.doctorExternalPractitionerId === practitionerId && 
      a.patientId === this.patient?.patientId && 
      a.status === 'COMPLETED'
    );
    if (!hasCompleted) {
      return { 
        blocked: true, 
        reason: this.prefs.translate('PATIENT.MESSAGES.LIMITS.NO_COMPLETED_APPOINTMENT')
      };
    }

    // Find existing conversation (if any) to check anti-spam limits
    const convId = `practitioner_${practitionerId}_concerned_${concernedPersonId}`;
    const conv = this.conversations.find(c => c.id === convId);
    if (!conv) {
      return { blocked: false, reason: '' };
    }

    // 2. Check consecutive limit (max 3 consecutive messages without doctor reply)
    let consecutive = 0;
    for (let i = conv.messages.length - 1; i >= 0; i--) {
      if (conv.messages[i].senderPatientId != null) {
        consecutive++;
      } else {
        break;
      }
    }
    if (consecutive >= 3) {
      return { 
        blocked: true, 
        reason: this.prefs.translate('PATIENT.MESSAGES.LIMITS.CONSECUTIVE_BLOCKED')
      };
    }

    // 3. Check daily limit (max 10 messages in the last 24 hours)
    const limitTime = Date.now() - 24 * 60 * 60 * 1000;
    const daily = conv.messages.filter(m => 
      m.senderPatientId != null && new Date(m.sentAt).getTime() > limitTime
    ).length;
    if (daily >= 10) {
      return { 
        blocked: true, 
        reason: this.prefs.translate('PATIENT.MESSAGES.LIMITS.DAILY_LIMIT_REACHED')
      };
    }

    return { blocked: false, reason: '' };
  }

  get chatLimitsCheck(): { blocked: boolean; reason: string } {
    if (!this.activeConversation) return { blocked: false, reason: '' };
    return this.checkLimitsForPractitionerAndConcerned(
      this.activeConversation.contactId,
      this.activeConversation.concernedPersonId
    );
  }

  get composeLimitsCheck(): { blocked: boolean; reason: string } {
    return this.checkLimitsForPractitionerAndConcerned(
      this.selectedPractitionerExternalId,
      this.concernTargetId
    );
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
    return this.prefs.translate(`PATIENT.MESSAGES.SUBJECT.${s}`);
  }

  canGoStep2(): boolean { return this.concernTargetId != null; }
  canGoStep3(): boolean { return this.selectedPractitionerExternalId != null && !this.composeLimitsCheck.blocked; }
  canGoStep4(): boolean {
    if (this.selectedSubject === 'AUTRE') return !!this.customSubject.trim();
    return this.selectedSubject != null;
  }

  canAdvanceFromCurrentStep(): boolean {
    switch (this.wizardStep) {
      case 1: return this.canGoStep2();
      case 2: return this.canGoStep3();
      case 3: return this.canGoStep4();
      default: return false;
    }
  }

  nextStep(): void {
    if (!this.canAdvanceFromCurrentStep() || this.wizardStep >= 4) return;
    this.wizardStep++;
  }

  prevStep(): void {
    if (this.wizardStep > 1) this.wizardStep--;
  }

  stepCounterLabel(total = 4): string {
    return this.prefs
      .translate('PATIENT.MESSAGES.STEP_COUNTER')
      .replace('{{current}}', String(this.wizardStep))
      .replace('{{total}}', String(total));
  }

  onComposeFiles(ev: Event): void {
    const el = ev.target as HTMLInputElement;
    if (el.files) {
      Array.from(el.files).forEach(file => {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          const dataUrl = e.target.result;
          const base64Index = dataUrl.indexOf('base64,') + 7;
          this.composeFiles.push({
            file: file,
            base64: dataUrl.substring(base64Index)
          });
        };
        reader.readAsDataURL(file);
      });
    }
    el.value = '';
  }

  removeComposeFile(i: number): void {
    this.composeFiles.splice(i, 1);
  }

  filesCountLabel(): string {
    const n = this.composeFiles.length;
    if (n <= 0) return '';
    if (n === 1) return this.prefs.translate('PATIENT.MESSAGES.FILES.ONE');
    return this.prefs.translate('PATIENT.MESSAGES.FILES.MANY').replace('{{n}}', String(n));
  }

  canSubmit(): boolean {
    const subjectOk = this.selectedSubject === 'AUTRE' ? !!this.customSubject.trim() : !!this.selectedSubject;
    return (
      (!!this.messageContent.trim() || this.composeFiles.length > 0) &&
      this.concernTargetId != null &&
      this.selectedPractitionerExternalId != null &&
      subjectOk &&
      !this.sending
    );
  }

  submit(): void {
    if (!this.patient || !this.canSubmit()) return;
    this.sendError = '';
    this.sending = true;
    
    const finalContent = this.selectedSubject === 'AUTRE'
      ? `[${this.customSubject.trim()}]\n\n${this.messageContent.trim()}`
      : (this.messageContent.trim() || ' ');

    const attachments: AttachmentPayload[] = this.composeFiles.map(c => ({
      fileName: c.file.name,
      fileType: c.file.type || 'application/octet-stream',
      base64Data: c.base64
    }));

    this.messaging
      .sendMessage({
        direction: 'PATIENT_TO_PRACTITIONER',
        concernedPersonId: this.concernTargetId!,
        receiverPractitionerProfileId: this.selectedPractitionerExternalId!,
        subject: this.selectedSubject!,
        content: finalContent,
        attachments: attachments
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (msg) => {
          this.sending = false;
          // Recharger tous les messages pour tout reconstruire proprement (ou on l'insère manuellement)
          this.messaging.getPatientMessages(this.patient!.patientId).subscribe(msgs => {
            this.groupMessagesIntoConversations(msgs);
            // Ouvrir la conversation correspondante
            const convId = `practitioner_${this.selectedPractitionerExternalId}_concerned_${this.concernTargetId}`;
            this.activeConversation = this.conversations.find(c => c.id === convId) || null;
            
            this.resetWizard();
            this.view = 'chat';
          });
        },
        error: (e) => {
          this.sending = false;
          this.sendError = formatHttpError(e, this.prefs.translate('COMMON.SERVICE_UNAVAILABLE'));
        },
      });
  }

  openCompose(): void {
    this.resetWizard();
    this.view = 'compose';
  }

  cancelCompose(): void {
    this.view = 'chat';
  }

  resetWizard(): void {
    this.wizardStep = 1;
    this.concernTargetType = null;
    this.concernTargetId = null;
    this.selectedPractitionerExternalId = null;
    this.selectedSubject = null;
    this.customSubject = '';
    this.messageContent = '';
    this.composeFiles = [];
    this.sendError = '';
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

  concernedLabel(m: ChatConversation): string {
    const me = this.patient?.patientId;
    if (me != null && m.concernedPersonId === me) return this.prefs.translate('PATIENT.MESSAGES.CONCERNED.ME');
    const pr = this.proches.find((x) => x.id === m.concernedPersonId);
    if (pr) return `${pr.prenom} ${pr.nom}`;
    return this.prefs.translate('PATIENT.MESSAGES.CONCERNED.RELATIVE');
  }
}

