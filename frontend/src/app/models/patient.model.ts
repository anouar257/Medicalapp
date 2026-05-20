export type Sexe = 'HOMME' | 'FEMME';

export interface Patient {
  id: number;
  sexe: Sexe;
  prenom: string;
  nom: string;
  dateNaissance: string;
  email: string;
  telephone: string;
  emailVerifie: boolean;
  telephoneVerifie: boolean;
  cguAcceptees: boolean;
  actif: boolean;
  dateInscription: string;
}

export interface RegisterRequest {
  sexe: Sexe;
  prenom: string;
  nom: string;
  dateNaissance: string;
  email: string;
  telephone: string;
  motDePasse: string;
  cguAcceptees: boolean;
}

export interface LoginRequest {
  identifiant: string;
  motDePasse: string;
}

export interface AuthResponse {
  token: string;
  patientId: number;
  email: string;
  prenom: string;
  nom: string;
  sexe: Sexe;
  dateNaissance: string;
  telephone: string;
  emailVerifie: boolean;
  telephoneVerifie: boolean;
}

/** Réponse POST `/api/auth/verify-otp` (champs session optionnels si Bearer présent). */
export interface VerifyOtpApiResponse {
  verified: boolean;
  message?: string;
  error?: string;
  token?: string;
  patientId?: number;
  email?: string;
  prenom?: string;
  nom?: string;
  sexe?: Sexe;
  dateNaissance?: string;
  telephone?: string;
  emailVerifie?: boolean;
  telephoneVerifie?: boolean;
}

export interface SendOtpRequest {
  to: string;
  channel: 'sms' | 'email';
}

export interface VerifyOtpRequest {
  to: string;
  code: string;
  channel: 'sms' | 'email';
}

export interface ForgotPasswordRequest {
  identifiant: string;
  channel: 'email' | 'sms';
}

export interface ResetPasswordRequest {
  token: string;
  nouveauMotDePasse: string;
}
