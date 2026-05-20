import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';

/** Choisit la page d'accueil de l'espace cabinet selon le rôle (assistant → demandes). */
@Component({
  standalone: true,
  template: '',
})
export class CabinetDefaultRedirectComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authPro = inject(AuthProService);

  ngOnInit(): void {
    const role = this.authPro.getRole();
    if (role === 'ADMIN' && this.authPro.getCurrentUser()?.organizationId == null) {
      this.router.navigate(['/platform-admin'], { replaceUrl: true });
    } else if (role === 'ASSISTANT') {
      this.router.navigate(['/cabinet/demandes'], { replaceUrl: true });
    } else {
      this.router.navigate(['/cabinet/dashboard'], { replaceUrl: true });
    }
  }
}
