import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest, Sexe } from '../../models/patient.model';
import { PreferencesService } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  readonly prefs = inject(PreferencesService);
  step = 1;
  registerData: RegisterRequest = {
    sexe: '' as Sexe,
    prenom: '',
    nom: '',
    dateNaissance: '',
    email: '',
    telephone: '',
    motDePasse: '',
    cguAcceptees: false
  };
  confirmPassword = '';
  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  checkExistence(): void {
    this.loading = true;
    this.errorMessage = '';

    this.authService.checkExistence(this.registerData.email, this.registerData.telephone).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.exists) {
          this.errorMessage = this.prefs.translate('auth.register.errExists');
        } else {
          this.step = 2;
        }
      },
      error: () => {
        this.loading = false;
        this.errorMessage = this.prefs.translate('auth.register.errCheck');
      }
    });
  }

  goToStep3(): void {
    this.errorMessage = '';
    if (this.registerData.motDePasse !== this.confirmPassword) {
      this.errorMessage = this.prefs.translate('auth.register.errPasswordMatch');
      return;
    }
    if (this.registerData.motDePasse.length < 8) {
      this.errorMessage = this.prefs.translate('auth.register.errPasswordLength');
      return;
    }
    this.step = 3;
  }

  onRegister(): void {
    this.loading = true;
    this.errorMessage = '';

    this.authService.register(this.registerData).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = this.prefs.translate('auth.register.success');
        setTimeout(() => {
          this.router.navigate(['/auth/verify-otp'], {
            queryParams: {
              email: this.registerData.email,
              telephone: this.registerData.telephone
            }
          });
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.error || this.prefs.translate('auth.register.errRegister');
      }
    });
  }
}
