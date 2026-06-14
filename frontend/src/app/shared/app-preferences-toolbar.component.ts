import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PreferencesService } from '../services/preferences.service';
import { ThemeService } from '../services/theme.service';
import type { AppLanguage, ZoomLevel } from '../services/preferences.service';

@Component({
  selector: 'app-preferences-toolbar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="flex flex-wrap items-center justify-end gap-2 text-sm"
      [class.flex-row-reverse]="prefs.language() === 'ar'"
    >
      <div class="flex items-center gap-1 rounded-full border border-slate-200/80 bg-white/80 px-1 py-0.5 dark:border-slate-700 dark:bg-slate-900/80">
        @for (lang of langs; track lang.code) {
          <button
            type="button"
            (click)="prefs.setLanguage(lang.code)"
            [class.bg-blue-600]="prefs.language() === lang.code"
            [class.text-white]="prefs.language() === lang.code"
            [class.text-slate-600]="prefs.language() !== lang.code"
            class="rounded-full px-2 py-1 text-xs font-semibold transition duration-300 dark:text-slate-300"
          >
            {{ lang.label }}
          </button>
        }
      </div>
      <div class="hidden sm:flex items-center gap-1 rounded-full border border-slate-200/80 bg-white/80 px-1 py-0.5 dark:border-slate-700 dark:bg-slate-900/80">
        @for (z of zooms; track z) {
          <button
            type="button"
            (click)="prefs.setZoom(z)"
            [class.bg-slate-800]="prefs.zoomLevel() === z"
            [class.text-white]="prefs.zoomLevel() === z"
            class="rounded-full px-2 py-0.5 text-xs font-bold text-slate-600 transition duration-300 dark:text-slate-300"
          >
            A
          </button>
        }
      </div>
      <button
        type="button"
        (click)="theme.toggle()"
        class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-200/80 bg-white/80 text-slate-700 transition duration-300 hover:scale-105 dark:border-slate-700 dark:bg-slate-900/80 dark:text-amber-300"
        [attr.aria-label]="prefs.translate('COMMON.TOGGLE_THEME')"
      >
        @if (theme.isDark()) {
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="4" />
            <path
              d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
            />
          </svg>
        } @else {
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
        }
      </button>
    </div>
  `,
})
export class AppPreferencesToolbarComponent {
  readonly prefs = inject(PreferencesService);
  readonly theme = inject(ThemeService);

  readonly langs: { code: AppLanguage; label: string }[] = [
    { code: 'fr', label: 'FR' },
    { code: 'en', label: 'EN' },
    { code: 'ar', label: 'ع' },
  ];

  readonly zooms: ZoomLevel[] = ['faible', 'moyen', 'fort'];
}
