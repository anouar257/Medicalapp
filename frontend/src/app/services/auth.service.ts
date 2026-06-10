import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  SendOtpRequest,
  VerifyOtpRequest,
  VerifyOtpApiResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest
} from '../models/patient.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.patientApiBaseUrl}/api/auth`;
  private readonly TOKEN_KEY = 'patient_jwt_token';
  private readonly PATIENT_KEY = 'patient_data';

  private currentPatientSubject = new BehaviorSubject<AuthResponse | null>(this.loadStoredPatient());
  public currentPatient$ = this.currentPatientSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ── Existence ──────────────────────────────────────────────────────────

  checkExistence(email: string, telephone: string): Observable<{ exists: boolean }> {
    return this.http.post<{ exists: boolean }>(`${this.apiUrl}/check-existence`, { email, telephone });
  }

  // ── Inscription ────────────────────────────────────────────────────────

  register(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, request);
  }

  // ── Connexion ──────────────────────────────────────────────────────────

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        localStorage.setItem(this.PATIENT_KEY, JSON.stringify(response));
        this.currentPatientSubject.next(response);
      })
    );
  }

  // ── OTP ────────────────────────────────────────────────────────────────

  sendOtp(request: SendOtpRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/send-otp`, request);
  }

  verifyOtp(request: VerifyOtpRequest): Observable<VerifyOtpApiResponse> {
    return this.http.post<VerifyOtpApiResponse>(`${this.apiUrl}/verify-otp`, request);
  }

  // ── Mot de passe oublié ────────────────────────────────────────────────

  forgotPassword(request: ForgotPasswordRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, request);
  }

  // ── Session ────────────────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.PATIENT_KEY);
    this.currentPatientSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return token !== null && token.length > 0;
  }

  getCurrentPatient(): AuthResponse | null {
    return this.currentPatientSubject.value;
  }

  /**
   * Met à jour la session après OTP (nouveau JWT avec claim isVerified) sans refaire un login.
   */
  applySessionAfterOtpVerify(body: VerifyOtpApiResponse): void {
    if (!body.token || body.patientId == null || !body.email) {
      return;
    }
    const cur = this.getCurrentPatient();
    const next: AuthResponse = {
      token: body.token,
      patientId: body.patientId,
      email: body.email,
      prenom: body.prenom ?? cur?.prenom ?? '',
      nom: body.nom ?? cur?.nom ?? '',
      sexe: body.sexe ?? cur?.sexe ?? 'HOMME',
      dateNaissance: body.dateNaissance ?? cur?.dateNaissance ?? '',
      telephone: body.telephone ?? cur?.telephone ?? '',
      emailVerifie: body.emailVerifie ?? true,
      telephoneVerifie: body.telephoneVerifie ?? cur?.telephoneVerifie ?? false,
      ville: cur?.ville ?? '',
    };
    localStorage.setItem(this.TOKEN_KEY, next.token);
    localStorage.setItem(this.PATIENT_KEY, JSON.stringify(next));
    this.currentPatientSubject.next(next);
  }

  private loadStoredPatient(): AuthResponse | null {
    const stored = localStorage.getItem(this.PATIENT_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as AuthResponse;
      } catch {
        return null;
      }
    }
    return null;
  }
}
