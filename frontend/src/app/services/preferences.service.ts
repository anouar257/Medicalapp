import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type ZoomLevel = 'faible' | 'moyen' | 'fort';
export type AppLanguage = 'fr' | 'en' | 'ar';

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly doc = inject(DOCUMENT);
  private readonly translateService = inject(TranslateService);

  readonly zoomLevel = signal<ZoomLevel>('moyen');
  readonly language = signal<AppLanguage>('fr');

  constructor() {
    this.translateService.setFallbackLang('fr');
    this.loadPreferences();
    this.applyZoom();
    this.applyLanguage();
  }

  setZoom(level: ZoomLevel) {
    this.zoomLevel.set(level);
    this.save('medconnect-zoom', level);
    this.applyZoom();
  }

  setLanguage(lang: AppLanguage) {
    this.language.set(lang);
    this.save('medconnect-lang', lang);
    this.applyLanguage();
  }

  /** Traduction globale via ngx-translate. */
  translate(key: string): string {
    return this.translateService.instant(key);
  }

  private loadPreferences() {
    if (typeof localStorage !== 'undefined') {
      const storedZoom = localStorage.getItem('medconnect-zoom') as ZoomLevel;
      if (storedZoom === 'faible' || storedZoom === 'moyen' || storedZoom === 'fort') {
        this.zoomLevel.set(storedZoom);
      }
      
      const storedLang = localStorage.getItem('medconnect-lang') as AppLanguage;
      if (storedLang === 'fr' || storedLang === 'en' || storedLang === 'ar') {
        this.language.set(storedLang);
      }
    }
  }

  private save(key: string, value: string) {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(key, value);
    }
  }

  private applyZoom() {
    const html = this.doc.documentElement;
    // Remove old zoom classes
    html.classList.remove('zoom-faible', 'zoom-moyen', 'zoom-fort');
    // Add new zoom class
    html.classList.add(`zoom-${this.zoomLevel()}`);
  }

  private applyLanguage() {
    const lang = this.language();
    this.translateService.use(lang);
    
    const html = this.doc.documentElement;
    html.lang = lang;
    
    const dir = lang === 'ar' ? 'rtl' : 'ltr';
    this.doc.dir = dir;
    html.dir = dir;
  }
}
