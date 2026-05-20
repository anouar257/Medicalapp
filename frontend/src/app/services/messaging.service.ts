import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';
import type {
  MessageResponseDto,
  MessagingAttachmentPayload,
  SendMessagePayload,
} from '../models/messaging.model';

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.messagingApiBaseUrl.replace(/\/$/, '');

  getPatientMessages(patientId: number): Observable<MessageResponseDto[]> {
    return this.http.get<MessageResponseDto[]>(`${this.base}/api/messages/patient/${patientId}`);
  }

  getPractitionerMessages(practitionerProfileId: number): Observable<MessageResponseDto[]> {
    return this.http.get<MessageResponseDto[]>(
      `${this.base}/api/messages/practitioner/${practitionerProfileId}`,
    );
  }

  markAsRead(messageId: number): Observable<MessageResponseDto> {
    return this.http.patch<MessageResponseDto>(`${this.base}/api/messages/${messageId}/read`, {});
  }

  sendMessage(payload: SendMessagePayload): Observable<MessageResponseDto> {
    return this.http.post<MessageResponseDto>(`${this.base}/api/messages`, payload);
  }

  /** Télécharge une pièce jointe. */
  downloadAttachment(messageId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.base}/api/messages/${messageId}/attachments/${attachmentId}`, {
      responseType: 'blob',
    });
  }

  async buildAttachmentsFromFiles(files: File[]): Promise<MessagingAttachmentPayload[]> {
    const out: MessagingAttachmentPayload[] = [];
    for (const file of files) {
      const base64Data = await this.fileToBase64(file);
      out.push({
        fileName: file.name,
        fileType: file.type || 'application/octet-stream',
        base64Data,
      });
    }
    return out;
  }

  private fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const res = reader.result as string;
        const comma = res.indexOf(',');
        resolve(comma >= 0 ? res.slice(comma + 1) : res);
      };
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  sendMessageWithFiles(
    payload: Omit<SendMessagePayload, 'attachments'>,
    files: File[],
  ): Observable<MessageResponseDto> {
    return from(this.buildAttachmentsFromFiles(files)).pipe(
      switchMap((attachments) =>
        this.sendMessage({ ...payload, attachments: attachments.length ? attachments : [] }),
      ),
    );
  }
}
