import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PreferencesService } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
})
export class ForgotPasswordComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authService = inject(AuthService);

  channel: 'email' | 'sms' = 'email';
  identifiant = '';
  loading = false;
  err = '';
  success = '';

  onSubmit() {
    this.loading = true;
    this.err = '';
    this.success = '';
    this.authService.forgotPassword({ identifiant: this.identifiant, channel: this.channel }).subscribe({
      next: () => {
        this.loading = false;
        this.success =
          this.channel === 'email'
            ? this.prefs.translate('auth.forgot.successEmail')
            : this.prefs.translate('auth.forgot.successSms');
      },
      error: (e) => {
        this.loading = false;
        this.err = e.error?.error || this.prefs.translate('auth.forgot.genericError');
      },
    });
  }
}
