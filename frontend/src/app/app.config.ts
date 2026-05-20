import { ApplicationConfig, provideAppInitializer, provideZoneChangeDetection, inject } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { APP_ROUTES } from './app.routes';
import { ThemeService } from './services/theme.service';
import { PreferencesService } from './services/preferences.service';
import { authInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(APP_ROUTES),
    provideAppInitializer(() => {
      inject(ThemeService).syncFromStorage();
      inject(PreferencesService);
    }),
  ],
};
