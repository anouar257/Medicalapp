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

// ---- Modèles ajoutés pour l'interface de messagerie style Chat WhatsApp ----

export interface AttachmentPayload {
  fileName: string;
  fileType: string;
  base64Data: string;
}

export interface ChatConversation {
  id: string; // Un identifiant unique pour regrouper (ex: 'patient_1_practitioner_2')
  contactName: string; // Nom de la personne avec qui on discute
  contactId: number; // L'ID du contact (patientId pour le docteur, practitionerProfileId pour le patient)
  concernedPersonId: number; // L'ID de la personne concernée par les soins
  messages: MessageResponseDto[]; // Liste triée de tous les messages de la conversation
  lastMessage: MessageResponseDto; // Le message le plus récent (pour l'aperçu)
  unreadCount: number; // Nombre de messages non lus
}
