import { Routes } from '@angular/router';

import { AdminDoctorsPageComponent } from './components/admin-doctors-page/admin-doctors-page.component';
import { AgendaDashboardComponent } from './components/agenda-dashboard/agenda-dashboard.component';

export const APP_ROUTES: Routes = [
  { path: '', component: AgendaDashboardComponent },
  { path: 'admin', pathMatch: 'full', redirectTo: '/admin/doctors' },
  { path: 'admin/doctors', component: AdminDoctorsPageComponent },
  { path: '**', redirectTo: '', pathMatch: 'full' },
];
