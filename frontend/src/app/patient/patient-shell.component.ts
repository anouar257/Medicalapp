import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthResponse } from '../models/patient.model';
import { PreferencesService } from '../services/preferences.service';
import { AppPreferencesToolbarComponent } from '../shared/app-preferences-toolbar.component';

@Component({
  selector: 'app-patient-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, AppPreferencesToolbarComponent],
  template: `
    <div class="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white transition-colors duration-300 flex flex-col">
      <nav class="sticky top-0 z-50 backdrop-blur-md bg-white/80 dark:bg-slate-900/90 border-b border-slate-200 dark:border-slate-800 transition-colors duration-300 shrink-0">
        <div class="max-w-7xl mx-auto flex items-center justify-between px-4 py-3 gap-3">
          <a routerLink="/patient/dashboard" class="flex items-center gap-3 min-w-0 hover:opacity-90 transition-opacity">
            <span class="text-3xl shrink-0">🏥</span>
            <h1 class="text-xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent truncate">
              MedConnect
            </h1>
            <span class="hidden sm:inline-block shrink-0 px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-[10px] font-bold uppercase tracking-wider">
              {{ prefs.translate('Patient') }}
            </span>
          </a>
          <div class="flex items-center gap-2 sm:gap-3 shrink-0" [class.flex-row-reverse]="prefs.language() === 'ar'">
            <a routerLink="/patient/prendre-rendez-vous" 
               class="hidden md:flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-black shadow-lg shadow-blue-500/20 transition-all duration-300 hover:scale-105 active:scale-95">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4" /></svg>
              {{ prefs.translate('Prendre RDV') }}
            </a>
            <app-preferences-toolbar />
            @if (patient) {
              <div class="flex items-center gap-3">
                <div class="hidden sm:flex flex-col items-end text-end" [class.items-start]="prefs.language() === 'ar'" [class.text-start]="prefs.language() === 'ar'">
                  <span class="text-sm font-bold text-slate-700 dark:text-slate-200">
                    {{ patient.prenom }} {{ patient.nom }}
                  </span>
                  <span class="text-[10px] text-slate-400">{{ prefs.translate('Compte vérifié') }}</span>
                </div>
                <button (click)="logout()" type="button" class="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 transition-colors duration-300" [attr.title]="prefs.translate('Déconnexion')">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
                </button>
              </div>
            }
          </div>
        </div>
      </nav>

      <div class="flex-1 flex flex-col min-h-0">
        <router-outlet></router-outlet>
      </div>
    </div>
  `
})
export class PatientShellComponent implements OnInit {
  patient: AuthResponse | null = null;
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly prefs = inject(PreferencesService);

  ngOnInit() {
    this.patient = this.authService.getCurrentPatient();
    if (!this.patient) {
      this.router.navigate(['/auth/login']);
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
