import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';
import { environment } from '../../environments/environment';
import {
  AuthProResponse,
  ForgotPasswordProRequest,
  LoginProRequest,
  ProUserRole,
  RegisterCabinetRequest,
  RegisterPractitionerRequest,
  ResetPasswordProRequest,
  SendOtpProRequest,
  VerifyOtpProRequest,
  VerifyOtpProApiResponse,
} from '../models/practitioner.model';

/**
 * Service d'authentification professionnelle (cabinet / praticien / assistant / ...).
 *
 * <p>État de session exposé en <strong>signals</strong> pour réagir sans F5 lors des changements
 * de {@code organizationId} ou {@code practitionerProfileId}. {@link currentUser$} reste disponible
 * pour le code basé sur RxJS.
 */
@Injectable({ providedIn: 'root' })
export class AuthProService {
  private readonly apiUrl = `${environment.practitionerApiBaseUrl}/api/pro/auth`;
  private readonly TOKEN_KEY = 'pro_jwt_token';
  private readonly USER_KEY = 'pro_user_data';

  private readonly _currentUser = signal<AuthProResponse | null>(this.loadStored());

  /** Utilisateur pro courant (null si déconnecté). */
  readonly currentUser = this._currentUser.asReadonly();

  readonly organizationId = computed(() => this._currentUser()?.organizationId ?? null);

  readonly practitionerProfileId = computed(() => this._currentUser()?.practitionerProfileId ?? null);

  /** Clé stable pour comparer deux contextes cabinet / profil. */
  readonly workspaceContextKey = computed(
    () => `${this.organizationId() ?? 'null'}|${this.practitionerProfileId() ?? 'null'}`,
  );

  /** Flux RxJS aligné sur le signal (migration douce depuis BehaviorSubject). */
  readonly currentUser$: Observable<AuthProResponse | null>;

  constructor(private http: HttpClient) {
    this.currentUser$ = toObservable(this._currentUser);
  }

  // ── Existence ──────────────────────────────────────────────────────────

  checkExistence(email: string, telephone: string): Observable<{ exists: boolean }> {
    return this.http.post<{ exists: boolean }>(`${this.apiUrl}/check-existence`, {
      email,
      telephone,
    });
  }

  // ── Inscription cabinet ────────────────────────────────────────────────

  registerCabinet(request: RegisterCabinetRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register-cabinet`, request);
  }

  // ── Inscription praticien ──────────────────────────────────────────────

  registerPractitioner(request: RegisterPractitionerRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register-practitioner`, request);
  }

  // ── Login (rôle renvoyé dans le JWT) ──────────────────────────────────

  login(request: LoginProRequest): Observable<AuthProResponse> {
    return this.http.post<AuthProResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((response) => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(response));
        this._currentUser.set(response);
      }),
    );
  }

  // ── OTP ────────────────────────────────────────────────────────────────

  sendOtp(req: SendOtpProRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/send-otp`, req);
  }

  verifyOtp(req: VerifyOtpProRequest): Observable<VerifyOtpProApiResponse> {
    return this.http.post<VerifyOtpProApiResponse>(`${this.apiUrl}/verify-otp`, req);
  }

  // ── Reset password ─────────────────────────────────────────────────────

  forgotPassword(req: ForgotPasswordProRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, req);
  }

  resetPassword(req: ResetPasswordProRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, req);
  }

  // ── Session ────────────────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._currentUser.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): AuthProResponse | null {
    return this._currentUser();
  }

  getRole(): ProUserRole | null {
    return this._currentUser()?.role ?? null;
  }

  applySessionAfterOtpVerify(body: VerifyOtpProApiResponse): void {
    if (!body.token || body.userId == null || !body.email || !body.role) {
      return;
    }
    const cur = this.getCurrentUser();
    const next: AuthProResponse = {
      token: body.token,
      userId: body.userId,
      email: body.email,
      telephone: body.telephone ?? cur?.telephone ?? '',
      prenom: body.prenom ?? cur?.prenom ?? '',
      nom: body.nom ?? cur?.nom ?? '',
      role: body.role,
      organizationId: body.organizationId ?? cur?.organizationId ?? null,
      organizationNom: body.organizationNom ?? cur?.organizationNom ?? null,
      emailVerifie: body.emailVerifie ?? true,
      telephoneVerifie: body.telephoneVerifie ?? cur?.telephoneVerifie ?? false,
      practitionerProfileId: body.practitionerProfileId ?? cur?.practitionerProfileId ?? null,
    };
    localStorage.setItem(this.TOKEN_KEY, next.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(next));
    this._currentUser.set(next);
  }

  /**
   * Met à jour la session locale (ex. après modification profil côté API) sans refaire un login complet.
   */
  patchCurrentUser(partial: Partial<AuthProResponse>): void {
    const cur = this._currentUser();
    if (!cur) return;
    const next = { ...cur, ...partial } as AuthProResponse;
    localStorage.setItem(this.USER_KEY, JSON.stringify(next));
    this._currentUser.set(next);
  }

  /**
   * Détermine la route de tableau de bord cible selon le rôle reçu du backend.
   */
  homeRouteForRole(role: ProUserRole | null | undefined): string {
    const u = this.getCurrentUser();
    if (role === 'ADMIN' && u?.organizationId == null) {
      return '/platform-admin';
    }
    switch (role) {
      case 'PRATICIEN':
        return '/cabinet/dashboard';
      case 'ASSISTANT':
        return '/cabinet/demandes';
      default:
        return '/cabinet/agenda';
    }
  }

  private loadStored(): AuthProResponse | null {
    const stored = localStorage.getItem(this.USER_KEY);
    if (!stored) return null;
    try {
      return JSON.parse(stored) as AuthProResponse;
    } catch {
      return null;
    }
  }
}
