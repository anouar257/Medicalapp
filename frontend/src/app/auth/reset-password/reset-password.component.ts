import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppNavbarComponent } from '../../shared/app-navbar.component';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  password = '';
  confirm = '';
  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  err = '';
  success = '';

  private destroyRef = inject(DestroyRef);

  constructor(private authService: AuthService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(p => this.token = p['token'] || '');
  }

  onSubmit() {
    this.err = '';
    if (this.password !== this.confirm) { this.err = 'Les mots de passe ne correspondent pas'; return; }
    if (this.password.length < 8) { this.err = 'Minimum 8 caractères'; return; }
    this.loading = true;
    this.authService.resetPassword({ token: this.token, nouveauMotDePasse: this.password }).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'Mot de passe réinitialisé !';
        setTimeout(() => this.router.navigate(['/auth/login']), 2000);
      },
      error: e => { this.loading = false; this.err = e.error?.error || 'Erreur'; }
    });
  }
}
