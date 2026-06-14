import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthProService } from '../../services/auth-pro.service';
import { LoginProRequest } from '../../models/practitioner.model';
import { PreferencesService } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

/**
 * Connexion pro — aucun choix de rôle.
 *
 * <p>L'utilisateur saisit uniquement email + mot de passe ; le backend lit le rôle
 * en base et le retourne dans le JWT. Le composant redirige ensuite vers le tableau
 * de bord adapté.
 */
@Component({
  selector: 'app-login-pro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './login-pro.component.html',
  styleUrls: ['./login-pro.component.scss'],
})
export class LoginProComponent implements OnInit, OnDestroy {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  loginData: LoginProRequest = { email: '', motDePasse: '' };
  loading = false;
  err = '';
  assistantMode = false;
  showPassword = false;
  private sub?: Subscription;

  ngOnInit(): void {
    this.sub = this.route.queryParamMap.subscribe((m) => {
      this.assistantMode = m.get('mode') === 'assistant';
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onSubmit() {
    this.err = '';
    if (!this.loginData.email || !this.loginData.motDePasse) {
      this.err = this.prefs.translate('AUTH.PRO.FILL_ALL');
      return;
    }

    this.loading = true;
    this.authPro.login(this.loginData).subscribe({
      next: (response) => {
        this.loading = false;
        const target = this.authPro.homeRouteForRole(response.role);
        this.router.navigate([target]);
      },
      error: (e) => {
        this.loading = false;
        this.err = e.error?.error || this.prefs.translate('AUTH.PRO.BAD_CREDENTIALS');
      },
    });
  }
}
