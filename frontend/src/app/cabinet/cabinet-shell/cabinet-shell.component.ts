import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';
import { ProUserRole } from '../../models/practitioner.model';
import { PreferencesService } from '../../services/preferences.service';
import { PractitionerService } from '../../services/practitioner.service';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';
import { resolveDoctorPhotoUrl } from '../../utils/media-url';
import { toObservable, toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, of, filter, switchMap } from 'rxjs';

interface NavItem {
  path: string;
  labelKey: string;
  icon: string;
  allowedRoles: ProUserRole[];
  /** Active uniquement si l'URL correspond exactement (utile pour les parents). */
  exact: boolean;
}

/**
 * Layout commun de l'espace cabinet — sidebar + outlet.
 * La sidebar n'affiche que les liens autorisés par le rôle de l'utilisateur connecté.
 */
@Component({
  selector: 'app-cabinet-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, AppPreferencesToolbarComponent],
  templateUrl: './cabinet-shell.component.html',
  styleUrls: ['./cabinet-shell.component.scss'],
})
export class CabinetShellComponent {
  readonly authPro = inject(AuthProService);
  private readonly router = inject(Router);
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);

  readonly orgName = computed(() => {
    this.prefs.language();
    const raw = this.authPro.currentUser()?.organizationNom?.trim();
    if (raw) return raw;
    return this.prefs.translate('cabinet.defaultOrg');
  });
  readonly userName = computed(() => {
    const u = this.authPro.currentUser();
    return u ? `${u.prenom} ${u.nom}` : '';
  });
  readonly userEmail = computed(() => this.authPro.currentUser()?.email ?? '');
  readonly roleLabel = computed(() => {
    this.prefs.language();
    const r = this.authPro.currentUser()?.role;
    return r ? this.prefs.translate(`role.${r}`) : '';
  });
  readonly initials = computed(() => {
    const u = this.authPro.currentUser();
    if (!u) return '?';
    return ((u.prenom?.[0] ?? '') + (u.nom?.[0] ?? '')).toUpperCase();
  });

  readonly userPhoto = signal<string | null>(null);

  constructor() {
    toObservable(this.authPro.currentUser).pipe(
      filter(u => !!u),
      switchMap(u => {
        if (u?.role === 'PRATICIEN') {
          return this.practitionerService.me().pipe(
            map(profile => ({ profile, user: u })),
            catchError(() => of({ profile: null, user: u }))
          );
        } else if (u?.role === 'ASSISTANT' && u.organizationId) {
          return this.practitionerService.listByOrganization(u.organizationId).pipe(
            map(profs => ({ profile: profs.length > 0 ? profs[0] : null, user: u })),
            catchError(() => of({ profile: null, user: u }))
          );
        }
        return of({ profile: null, user: u });
      }),
      takeUntilDestroyed()
    ).subscribe(({ profile, user }) => {
      // First try PractitionerProfile photo, then fallback to ProUser photo
      const photoUrl = profile?.photoUrl || (user as any)?.photoUrl;
      this.userPhoto.set(photoUrl ? resolveDoctorPhotoUrl(photoUrl) : null);
    });
  }

  private readonly NAV_ITEMS: NavItem[] = [
    { path: 'dashboard', labelKey: 'nav.cabinetOverview', icon: '⚙️', exact: true, allowedRoles: ['PRATICIEN'] },
    { path: '/agenda-cabinet', labelKey: 'nav.agenda', icon: '📅', exact: false, allowedRoles: ['PRATICIEN', 'ASSISTANT'] },
    { path: 'demandes', labelKey: 'nav.assistantRequests', icon: '⏳', exact: false, allowedRoles: ['PRATICIEN', 'ASSISTANT'] },
    { path: 'messages', labelKey: 'nav.messages', icon: '💬', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'payments', labelKey: 'docTitle.cabinetPayments', icon: '💳', exact: false, allowedRoles: ['PRATICIEN', 'ASSISTANT'] },
    { path: 'profile', labelKey: 'nav.profile', icon: '👤', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'diplomas', labelKey: 'nav.diplomas', icon: '🎓', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'verifications', labelKey: 'nav.verifications', icon: '🛡️', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'staff', labelKey: 'nav.staff', icon: '👥', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'practice', labelKey: 'nav.cabinet', icon: '🏥', exact: false, allowedRoles: ['PRATICIEN'] },
    { path: 'doctors', labelKey: 'docTitle.cabinetDoctors', icon: '🩺', exact: false, allowedRoles: ['PRATICIEN'] },
  ];

  readonly visibleNavItems = computed(() => {
    const role = this.authPro.currentUser()?.role;
    if (!role) return [];
    return this.NAV_ITEMS.filter((it) => it.allowedRoles.includes(role));
  });

  isActive(item: NavItem): boolean {
    const currentUrl = this.router.url;
    const fullPath = item.path.startsWith('/') ? item.path : '/cabinet/' + item.path;
    if (item.exact) {
      return currentUrl === fullPath || currentUrl.startsWith(fullPath + '?');
    }
    return currentUrl.startsWith(fullPath);
  }

  handleMenuClick(event: MouseEvent, item: NavItem) {
    event.preventDefault();
    const fullPath = item.path.startsWith('/') ? item.path : '/cabinet/' + item.path;
    // Hard refresh guarantees fresh data as requested
    window.location.href = fullPath;
  }

  logout() {
    this.authPro.logout();
    this.router.navigate(['/auth/login-pro']);
  }
}
