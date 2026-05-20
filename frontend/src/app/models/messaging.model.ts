/** Aligné sur messaging-service (Subject). */
export type MessagingSubject =
  | 'RESULTAT_EXAMEN'
  | 'QUESTION_AVANT_RDV'
  | 'QUESTION_APRES_RDV'
  | 'AUTRE';

export type MessagingDirection = 'PATIENT_TO_PRACTITIONER' | 'PRACTITIONER_TO_PATIENT';

export interface MessagingAttachmentPayload {
  fileName: string;
  fileType: string;
  base64Data: string;
}

export interface SendMessagePayload {
  direction: MessagingDirection;
  concernedPersonId: number;
  subject: MessagingSubject;
  content: string;
  receiverPractitionerProfileId?: number | null;
  receiverPatientId?: number | null;
  attachments?: MessagingAttachmentPayload[];
}

export interface MessagingAttachmentSummary {
  id: number;
  fileName: string;
  fileType: string;
}

export interface MessageResponseDto {
  id: number;
  senderPatientId: number | null;
  senderName?: string | null;
  senderPractitionerProfileId: number | null;
  receiverPatientId: number | null;
  receiverName?: string | null;
  receiverPractitionerProfileId: number | null;
  concernedPersonId: number;
  concernedPersonName?: string | null;
  content: string;
  subject: MessagingSubject;
  sentAt: string;
  read: boolean;
  attachments: MessagingAttachmentSummary[];
}
