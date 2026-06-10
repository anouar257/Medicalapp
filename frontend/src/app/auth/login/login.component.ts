import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/patient.model';
import { PreferencesService } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  loginData: LoginRequest = { identifiant: '', motDePasse: '' };
  loading = false;
  errorMessage = '';
  showPassword = false;

  onLogin(): void {
    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.loginData).subscribe({
      next: () => {
        const snap = this.route.snapshot.queryParamMap;
        const path = snap.get('returnUrl') || '/patient/dashboard';
        const qp: Record<string, string> = {};
        const pid = snap.get('practitionerId');
        const aid = snap.get('agendaDoctorId');
        if (pid) qp['practitionerId'] = pid;
        if (aid) qp['agendaDoctorId'] = aid;
        this.router.navigate([path], { queryParams: Object.keys(qp).length ? qp : undefined });
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.error || this.prefs.translate('Erreur de connexion');
      }
    });
  }
}
