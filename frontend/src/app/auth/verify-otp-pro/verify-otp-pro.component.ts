import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

/**
 * Vérification OTP côté pro (email + SMS via Twilio Verify).
 */
@Component({
  selector: 'app-verify-otp-pro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './verify-otp-pro.component.html',
  styleUrls: ['./verify-otp-pro.component.scss'],
})
export class VerifyOtpProComponent implements OnInit {
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

  constructor(
    private authPro: AuthProService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((p) => {
        this.email = p['email'] || '';
        this.telephone = p['telephone'] || '';
      });
  }

  verifyEmail() {
    this.loadingEmail = true;
    this.err = '';
    this.authPro.verifyOtp({ to: this.email, code: this.emailCode, channel: 'email' }).subscribe({
      next: (r) => {
        this.loadingEmail = false;
        this.authPro.applySessionAfterOtpVerify(r);
        this.emailVerified = true;
      },
      error: (e) => {
        this.loadingEmail = false;
        this.err = e.error?.error || 'Code invalide';
      },
    });
  }

  verifySms() {
    this.loadingSms = true;
    this.err = '';
    this.authPro.verifyOtp({ to: this.telephone, code: this.smsCode, channel: 'sms' }).subscribe({
      next: (r) => {
        this.loadingSms = false;
        this.authPro.applySessionAfterOtpVerify(r);
        this.smsVerified = true;
      },
      error: (e) => {
        this.loadingSms = false;
        this.err = e.error?.error || 'Code invalide';
      },
    });
  }

  resendEmail() {
    this.authPro.sendOtp({ to: this.email, channel: 'email' }).subscribe();
    this.emailCd = 60;
    const i = setInterval(() => {
      this.emailCd--;
      if (this.emailCd <= 0) clearInterval(i);
    }, 1000);
  }

  resendSms() {
    this.authPro.sendOtp({ to: this.telephone, channel: 'sms' }).subscribe();
    this.smsCd = 60;
    const i = setInterval(() => {
      this.smsCd--;
      if (this.smsCd <= 0) clearInterval(i);
    }, 1000);
  }

  goLogin() {
    this.router.navigate(['/auth/login-pro']);
  }
}
