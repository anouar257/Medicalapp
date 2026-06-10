import { Component, DestroyRef, inject, ElementRef, ViewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { AuthProService } from '../../services/auth-pro.service';
import { MessagingService } from '../../services/messaging.service';
import { PreferencesService } from '../../services/preferences.service';
import type { MessageResponseDto, MessagingSubject, ChatConversation, AttachmentPayload } from '../../models/messaging.model';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import { formatHttpError } from '../../utils/http-error-message';

@Component({
  selector: 'app-cabinet-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, AppPreferencesToolbarComponent],
  templateUrl: './cabinet-messages.component.html',
  styleUrls: ['./cabinet-messages.component.scss'],
})
export class CabinetMessagesComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);
  private readonly messaging = inject(MessagingService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;
  @ViewChild('fileInput') private fileInput!: ElementRef;

  profileId: number | null = null;
  
  conversations: ChatConversation[] = [];
  activeConversation: ChatConversation | null = null;
  
  replyText = '';
  loading = true;
  error = '';
  sending = false;
  sendError = '';

  // Pièce jointe sélectionnée
  selectedFile: File | null = null;
  selectedFileBase64: string | null = null;

  constructor() {
    toObservable(this.authPro.practitionerProfileId)
      .pipe(
        distinctUntilChanged(),
        switchMap((profileId) => {
          this.profileId = profileId;
          this.activeConversation = null;
          this.replyText = '';
          this.sendError = '';
          this.error = '';
          this.clearFile();

          if (profileId == null) {
            this.conversations = [];
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
        this.groupMessagesIntoConversations(list);
        this.loading = false;
      });
  }

  goToDashboard(): void {
    window.history.back();
  }

  private groupMessagesIntoConversations(list: MessageResponseDto[]): void {
    // Trier tous les messages chronologiquement (du plus ancien au plus récent)
    const sorted = [...list].sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime());
    
    const convMap = new Map<string, ChatConversation>();

    for (const m of sorted) {
      // Pour le praticien, l'interlocuteur est le patient
      const contactId = m.senderPatientId ?? m.receiverPatientId;
      if (!contactId) continue;

      const convId = `patient_${contactId}_concerned_${m.concernedPersonId}`;

      if (!convMap.has(convId)) {
        convMap.set(convId, {
          id: convId,
          contactId: contactId,
          concernedPersonId: m.concernedPersonId,
          contactName: this.extractContactName(m),
          messages: [],
          lastMessage: m,
          unreadCount: 0
        });
      }

      const conv = convMap.get(convId)!;
      conv.messages.push(m);
      conv.lastMessage = m; // Puisqu'on itère dans l'ordre chronologique, le dernier écrasera toujours
      
      // Compter les non-lus (si reçu par le praticien)
      if (!m.read && m.receiverPractitionerProfileId === this.profileId) {
        conv.unreadCount++;
      }
    }

    // Trier les conversations par date du dernier message (les plus récents en premier)
    this.conversations = Array.from(convMap.values()).sort(
      (a, b) => new Date(b.lastMessage.sentAt).getTime() - new Date(a.lastMessage.sentAt).getTime()
    );

    // Rafraîchir la conversation active si nécessaire
    if (this.activeConversation) {
      const updated = this.conversations.find(c => c.id === this.activeConversation!.id);
      if (updated) {
        this.activeConversation = updated;
        this.scrollToBottom();
      }
    }
  }

  private extractContactName(m: MessageResponseDto): string {
    if (m.senderPatientId != null && m.senderName) return m.senderName;
    if (m.receiverPatientId != null && m.receiverName) return m.receiverName;
    return `${this.prefs.translate('Patient')} #${m.senderPatientId ?? m.receiverPatientId}`;
  }

  selectConversation(c: ChatConversation): void {
    this.activeConversation = c;
    this.replyText = '';
    this.sendError = '';
    this.clearFile();

    // Marquer les messages comme lus
    let hasUnread = false;
    for (const m of c.messages) {
      if (!m.read && m.receiverPractitionerProfileId === this.profileId) {
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

  // --- Gestion des pièces jointes ---
  
  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        // Obtenir juste la partie Base64 après la virgule
        const dataUrl = e.target.result;
        const base64Index = dataUrl.indexOf('base64,') + 7;
        this.selectedFileBase64 = dataUrl.substring(base64Index);
      };
      reader.readAsDataURL(file);
    }
    event.target.value = ''; // Réinitialiser l'input
  }

  clearFile(): void {
    this.selectedFile = null;
    this.selectedFileBase64 = null;
  }

  sendReply(): void {
    if (!this.activeConversation || this.profileId == null) return;
    if (!this.replyText.trim() && !this.selectedFile) return; // Autoriser si juste une image sans texte

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
        direction: 'PRACTITIONER_TO_PATIENT',
        receiverPatientId: this.activeConversation.contactId,
        concernedPersonId: this.activeConversation.concernedPersonId,
        subject: 'QUESTION_APRES_RDV', // On garde le même sujet par défaut pour la discussion
        content: this.replyText.trim() || ' ', // Backend n'aime peut-être pas un texte vide, espace par défaut
        attachments: attachments,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (msg) => {
          this.sending = false;
          // L'ajouter au contexte local
          this.activeConversation!.messages.push(msg);
          this.activeConversation!.lastMessage = msg;
          // Remonter la conversation en haut
          this.conversations = this.conversations.sort(
            (a, b) => new Date(b.lastMessage.sentAt).getTime() - new Date(a.lastMessage.sentAt).getTime()
          );
          
          this.replyText = '';
          this.clearFile();
          setTimeout(() => this.scrollToBottom(), 50);
        },
        error: (e) => {
          this.sending = false;
          this.sendError = formatHttpError(e, this.prefs.translate('common.serviceUnavailable'));
        },
      });
  }
}

