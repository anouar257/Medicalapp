import { ApplicationConfig, provideAppInitializer, provideZoneChangeDetection, inject } from '@angular/core';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';

import { APP_ROUTES } from './app.routes';
import { ThemeService } from './services/theme.service';
import { PreferencesService } from './services/preferences.service';
import { authInterceptor } from './interceptors/auth.interceptor';
import { MultiTranslateHttpLoader } from './i18n/multi-translate-http-loader';

export function createTranslateLoader(http: HttpClient) {
  return new MultiTranslateHttpLoader(http);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(APP_ROUTES),
    provideTranslateService({
      fallbackLang: 'fr',
      loader: {
        provide: TranslateLoader,
        useFactory: createTranslateLoader,
        deps: [HttpClient],
      },
    }),
    provideAppInitializer(() => {
      inject(ThemeService).syncFromStorage();
      inject(PreferencesService);
    }),
  ],
};
