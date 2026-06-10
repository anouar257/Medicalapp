import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PreferencesService, AppLanguage } from '../services/preferences.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="sticky top-0 z-50 backdrop-blur-md bg-white/80 dark:bg-slate-900/80 border-b border-slate-200 dark:border-slate-800 transition-colors">
      <div class="max-w-7xl mx-auto flex items-center justify-between px-4 py-3">
        
        <!-- Logo -->
        <a routerLink="/" class="flex items-center gap-2">
          <span class="text-3xl">🩺</span>
          <span class="font-bold text-xl tracking-tight">MedConnect</span>
        </a>

        <!-- Accessibility Controls & Language -->
        <div class="flex items-center gap-2 sm:gap-4">
          
          <!-- Multilangue Dropdown -->
          <div class="relative group">
            <button class="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors text-sm font-bold">
              @if (prefs.language() === 'fr') { <span>🇫🇷 FR</span> }
              @else if (prefs.language() === 'en') { <span>🇬🇧 EN</span> }
              @else { <span>🇲🇦 ع</span> }
              <svg class="w-4 h-4 text-slate-400 group-hover:rotate-180 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path d="M19 9l-7 7-7-7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            
            <!-- Dropdown Menu -->
            <div class="absolute right-0 mt-1 w-32 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-slate-100 dark:border-slate-700 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform origin-top-right scale-95 group-hover:scale-100 z-[60]">
              <div class="p-1">
                <button (click)="prefs.setLanguage('fr')" class="w-full text-left px-3 py-2 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors flex items-center gap-2 text-sm">
                  <span>🇫🇷</span> Français
                </button>
                <button (click)="prefs.setLanguage('en')" class="w-full text-left px-3 py-2 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors flex items-center gap-2 text-sm">
                  <span>🇬🇧</span> English
                </button>
                <button (click)="prefs.setLanguage('ar')" class="w-full text-left px-3 py-2 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors flex items-center gap-2 text-sm">
                  <span>🇲🇦</span> العربية
                </button>
              </div>
            </div>
          </div>

          <!-- Zoom controls -->
          <div class="hidden sm:flex items-center bg-slate-100 dark:bg-slate-800 rounded-full p-1">
            <button (click)="prefs.setZoom('faible')" [class.bg-white]="prefs.zoomLevel() === 'faible'" [class.dark:bg-slate-600]="prefs.zoomLevel() === 'faible'" [class.shadow-sm]="prefs.zoomLevel() === 'faible'" class="px-2 py-1 text-xs rounded-full transition-all" title="Zoom Faible">A</button>
            <button (click)="prefs.setZoom('moyen')" [class.bg-white]="prefs.zoomLevel() === 'moyen'" [class.dark:bg-slate-600]="prefs.zoomLevel() === 'moyen'" [class.shadow-sm]="prefs.zoomLevel() === 'moyen'" class="px-2 py-1 text-sm rounded-full transition-all" title="Zoom Moyen">A</button>
            <button (click)="prefs.setZoom('fort')" [class.bg-white]="prefs.zoomLevel() === 'fort'" [class.dark:bg-slate-600]="prefs.zoomLevel() === 'fort'" [class.shadow-sm]="prefs.zoomLevel() === 'fort'" class="px-2 py-1 text-base rounded-full transition-all" title="Zoom Fort">A</button>
          </div>

          <!-- Dark Mode Button -->
          <button 
            (click)="theme.toggle()" 
            class="hover:bg-slate-100 dark:hover:bg-slate-800 p-2 rounded-full transition-colors flex items-center justify-center"
            title="Mode Sombre/Clair"
          >
            @if (theme.isDark()) {
              <svg class="w-5 h-5 text-amber-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="4"/>
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
              </svg>
            } @else {
              <svg class="w-5 h-5 text-slate-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
            }
          </button>

          <!-- Navigation Links -->
          <div class="hidden md:flex gap-3 ml-2">
            <a routerLink="/auth/login" class="px-4 py-2 text-sm font-semibold text-blue-600 border border-blue-600 dark:border-blue-500 dark:text-blue-400 rounded-full hover:bg-blue-50 dark:hover:bg-blue-900/30 transition">
              {{ translate('Espace Patient') }}
            </a>
            <a routerLink="/auth/login-pro" class="px-4 py-2 text-sm font-semibold text-white bg-blue-600 rounded-full hover:bg-blue-700 shadow-md shadow-blue-500/20 transition transform hover:-translate-y-0.5">
              {{ translate('Espace Pro') }}
            </a>
          </div>
        </div>
      </div>
    </header>
  `
})
export class AppNavbarComponent {
  readonly prefs = inject(PreferencesService);
  readonly theme = inject(ThemeService);

  private readonly dictionary: Record<string, Record<AppLanguage, string>> = {
    'Espace Patient': { fr: 'Espace Patient', en: 'Patient Portal', ar: 'بوابة المريض' },
    'Espace Pro': { fr: 'Espace Pro', en: 'Pro Portal', ar: 'بوابة الطبيب' }
  };

  translate(key: string): string {
    const lang = this.prefs.language();
    const row = this.dictionary[key];
    return row?.[lang] ?? key;
  }
}
