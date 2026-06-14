import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PreferencesService } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './verify-otp.component.html',
  styleUrls: ['./verify-otp.component.scss'],
})
export class VerifyOtpComponent implements OnInit {
  readonly prefs = inject(PreferencesService);
  email = '';
  telephone = '';
  emailCode = '';
  smsCode = '';
  emailVerified = false;
  smsVerified = false;
  loadingEmail = false;
  loadingSms = false;
  emailCd = 0;
  smsCd = 0;
  err = '';

  private readonly destroyRef = inject(DestroyRef);

  constructor(private authService: AuthService, private route: ActivatedRoute, public router: Router) {}

  ngOnInit() {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(p => { this.email = p['email'] || ''; this.telephone = p['telephone'] || ''; });
  }

  verifyEmail() {
    this.loadingEmail = true; this.err = '';
    this.authService.verifyOtp({ to: this.email, code: this.emailCode, channel: 'email' }).subscribe({
      next: (r) => {
        this.loadingEmail = false;
        this.authService.applySessionAfterOtpVerify(r);
        this.emailVerified = true;
      },
      error: e => { this.loadingEmail = false; this.err = e.error?.error || this.prefs.translate('AUTH.OTP.INVALID_CODE'); }
    });
  }

  verifySms() {
    this.loadingSms = true; this.err = '';
    this.authService.verifyOtp({ to: this.telephone, code: this.smsCode, channel: 'sms' }).subscribe({
      next: (r) => {
        this.loadingSms = false;
        this.authService.applySessionAfterOtpVerify(r);
        this.smsVerified = true;
      },
      error: e => { this.loadingSms = false; this.err = e.error?.error || this.prefs.translate('AUTH.OTP.INVALID_CODE'); }
    });
  }

  resendEmail() {
    this.authService.sendOtp({ to: this.email, channel: 'email' }).subscribe();
    this.emailCd = 60;
    const i = setInterval(() => { this.emailCd--; if (this.emailCd <= 0) clearInterval(i); }, 1000);
  }

  resendSms() {
    this.authService.sendOtp({ to: this.telephone, channel: 'sms' }).subscribe();
    this.smsCd = 60;
    const i = setInterval(() => { this.smsCd--; if (this.smsCd <= 0) clearInterval(i); }, 1000);
  }
}
