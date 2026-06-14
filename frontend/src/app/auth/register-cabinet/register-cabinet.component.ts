import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';
import { RegisterCabinetRequest } from '../../models/practitioner.model';
import { AppNavbarComponent } from '../../shared/app-navbar.component';
import { PreferencesService } from '../../services/preferences.service';

/**
 * Inscription d'un cabinet (organisme médical).
 *
 * <p>Crée :
 * <ul>
 *   <li>l'organisme médical (cabinet) ;</li>
 *   <li>le compte ADMIN racine du cabinet (le créateur).</li>
 *   <li>ou l'assistant.</li>
 * </ul>
 * <p>Après inscription, l'utilisateur est redirigé vers la page de vérification OTP.
 */
@Component({
  selector: 'app-register-cabinet',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './register-cabinet.component.html',
  styleUrls: ['./register-cabinet.component.scss'],
})
export class RegisterCabinetComponent {
  step = 1;
  data: RegisterCabinetRequest = {
    nomCabinet: '',
    prenom: '',
    nom: '',
    email: '',
    telephone: '',
    motDePasse: '',
    cguAcceptees: false,
  };
  confirmPassword = '';
  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private authPro: AuthProService,
    private router: Router,
    public prefs: PreferencesService
  ) {}

  goToStep2() {
    this.errorMessage = '';
    if (!this.data.nomCabinet?.trim()) {
      this.errorMessage = this.prefs.translate('AUTH.PRO.CABINET_NAME_REQUIRED');
      return;
    }
    this.step = 2;
  }

  goToStep3() {
    this.errorMessage = '';
    if (this.data.motDePasse !== this.confirmPassword) {
      this.errorMessage = this.prefs.translate('AUTH.REGISTER.ERR_PASSWORD_MATCH');
      return;
    }
    if (this.data.motDePasse.length < 8) {
      this.errorMessage = this.prefs.translate('AUTH.REGISTER.ERR_PASSWORD_LENGTH');
      return;
    }
    this.step = 3;
  }

  onSubmit() {
    this.loading = true;
    this.errorMessage = '';
    this.authPro.registerCabinet(this.data).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = this.prefs.translate('AUTH.PRO.CABINET_CREATED');
        setTimeout(() => {
          this.router.navigate(['/auth/verify-otp-pro'], {
            queryParams: { email: this.data.email, telephone: this.data.telephone },
          });
        }, 1500);
      },
      error: (e) => {
        this.loading = false;
        this.errorMessage = e.error?.error || this.prefs.translate('AUTH.REGISTER.ERR_REGISTER');
      },
    });
  }
}
