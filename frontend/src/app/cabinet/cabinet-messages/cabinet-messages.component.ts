import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { AuthProService } from '../../services/auth-pro.service';
import { MessagingService } from '../../services/messaging.service';
import { PreferencesService } from '../../services/preferences.service';
import type { MessageResponseDto, MessagingSubject } from '../../models/messaging.model';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import { formatHttpError } from '../../utils/http-error-message';

@Component({
  selector: 'app-cabinet-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, AppPreferencesToolbarComponent],
  templateUrl: './cabinet-messages.component.html',
  styleUrls: ['./cabinet-messages.component.scss'],
})
export class CabinetMessagesComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);
  private readonly messaging = inject(MessagingService);
  private readonly destroyRef = inject(DestroyRef);

  profileId: number | null = null;
  messages: MessageResponseDto[] = [];
  selected: MessageResponseDto | null = null;
  replyText = '';
  loading = true;
  error = '';
  sending = false;
  sendError = '';

  constructor() {
    toObservable(this.authPro.practitionerProfileId)
      .pipe(
        distinctUntilChanged(),
        switchMap((profileId) => {
          this.profileId = profileId;
          this.selected = null;
          this.replyText = '';
          this.sendError = '';
          this.error = '';
          if (profileId == null) {
            this.messages = [];
            this.loading = false;
            return of<MessageResponseDto[]>([]);
          }
          this.loading = true;
          return this.messaging.getPractitionerMessages(profileId).pipe(
            catchError(() => {
              this.loading = false;
              this.error = this.prefs.translate('common.serviceUnavailable');
              return of<MessageResponseDto[]>([]);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.messages = [...list].sort(
          (a, b) => new Date(b.sentAt).getTime() - new Date(a.sentAt).getTime(),
        );
        this.loading = false;
      });
  }

  selectMessage(m: MessageResponseDto): void {
    this.selected = m;
    this.replyText = '';
    this.sendError = '';
    if (!m.read && this.profileId != null && m.receiverPractitionerProfileId === this.profileId) {
      this.messaging
        .markAsRead(m.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (u) => Object.assign(m, u),
        });
    }
  }

  displayConcernedPerson(m: MessageResponseDto): string {
    const n = m.concernedPersonName?.trim();
    if (n) return n;
    return `#${m.concernedPersonId}`;
  }

  displaySender(m: MessageResponseDto): string {
    const n = m.senderName?.trim();
    if (n) return n;
    if (m.senderPatientId != null) return `${this.prefs.translate('Patient')} #${m.senderPatientId}`;
    if (m.senderPractitionerProfileId != null) {
      return `${this.prefs.translate('messaging.unknownDoctor')} #${m.senderPractitionerProfileId}`;
    }
    return '—';
  }

  displayReceiver(m: MessageResponseDto): string {
    const n = m.receiverName?.trim();
    if (n) return n;
    if (m.receiverPatientId != null) return `${this.prefs.translate('Patient')} #${m.receiverPatientId}`;
    if (m.receiverPractitionerProfileId != null) {
      return `${this.prefs.translate('messaging.unknownDoctor')} #${m.receiverPractitionerProfileId}`;
    }
    return '—';
  }

  subjectLabel(s: MessagingSubject): string {
    return this.prefs.translate(`messaging.subject.${s}`);
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

  sendReply(): void {
    if (!this.selected || !this.replyText.trim() || this.profileId == null) return;
    const receiverPatientId = this.selected.senderPatientId ?? this.selected.receiverPatientId;
    if (receiverPatientId == null) {
      this.sendError = this.prefs.translate('common.serviceUnavailable');
      return;
    }
    this.sending = true;
    this.sendError = '';
    this.messaging
      .sendMessage({
        direction: 'PRACTITIONER_TO_PATIENT',
        receiverPatientId,
        concernedPersonId: this.selected.concernedPersonId,
        subject: 'QUESTION_APRES_RDV',
        content: this.replyText.trim(),
        attachments: [],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (msg) => {
          this.sending = false;
          this.messages = [msg, ...this.messages];
          this.replyText = '';
          this.selected = msg;
        },
        error: (e) => {
          this.sending = false;
          this.sendError = formatHttpError(e, this.prefs.translate('common.serviceUnavailable'));
        },
      });
  }
}
