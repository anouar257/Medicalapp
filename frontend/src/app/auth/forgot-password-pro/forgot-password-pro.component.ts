import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';
import { PreferencesService } from '../../services/preferences.service';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';

@Component({
  selector: 'app-forgot-password-pro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppPreferencesToolbarComponent],
  templateUrl: './forgot-password-pro.component.html',
  styleUrls: ['./forgot-password-pro.component.scss'],
})
export class ForgotPasswordProComponent {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);

  channel: 'email' | 'sms' = 'email';
  identifiant = '';
  loading = false;
  err = '';
  success = '';

  onSubmit() {
    this.loading = true;
    this.err = '';
    this.success = '';
    this.authPro.forgotPassword({ identifiant: this.identifiant, channel: this.channel }).subscribe({
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
