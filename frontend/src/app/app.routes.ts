import { Routes } from '@angular/router';

import { AgendaDashboardComponent } from './components/agenda-dashboard/agenda-dashboard.component';
import { authGuard } from './guards/auth.guard';
import { authProGuard } from './guards/auth-pro.guard';
import { patientEmailVerifiedGuard } from './guards/patient-email-verified.guard';
import { proEmailVerifiedGuard } from './guards/pro-email-verified.guard';
import { cabinetSpaceGuard } from './guards/cabinet-space.guard';
import { roleGuard } from './guards/role.guard';
import { platformAdminGuard } from './guards/platform-admin.guard';
import { cabinetWorkspaceContextResolver } from './resolvers/cabinet-workspace-context.resolver';

export const APP_ROUTES: Routes = [
  {
    path: '',
    data: { pageTitleKey: 'docTitle.landing' },
    loadComponent: () =>
      import('./pages/landing-page/landing-page.component').then(m => m.LandingPageComponent),
  },

  {
    path: 'agenda-cabinet',
    component: AgendaDashboardComponent,
    canActivate: [authProGuard, proEmailVerifiedGuard, cabinetSpaceGuard],
    data: { pageTitleKey: 'docTitle.agendaLegacy' },
  },

  { path: 'admin', pathMatch: 'full', redirectTo: 'cabinet/doctors' },
  { path: 'admin/doctors', pathMatch: 'full', redirectTo: 'cabinet/doctors' },

  { path: 'super-admin', redirectTo: 'platform-admin', pathMatch: 'full' },
  {
    path: 'platform-admin',
    canActivate: [authProGuard, proEmailVerifiedGuard, platformAdminGuard],
    data: { pageTitleKey: 'docTitle.platformAdmin' },
    loadComponent: () =>
      import('./platform-admin/platform-dashboard/platform-dashboard.component').then(
        (m) => m.PlatformDashboardComponent,
      ),
  },

  {
    path: 'cabinet',
    canActivate: [authProGuard, proEmailVerifiedGuard, cabinetSpaceGuard],
    resolve: { workspace: cabinetWorkspaceContextResolver },
    loadComponent: () =>
      import('./cabinet/cabinet-shell/cabinet-shell.component').then(m => m.CabinetShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./cabinet/cabinet-default-redirect/cabinet-default-redirect.component').then(
            (m) => m.CabinetDefaultRedirectComponent,
          ),
      },
      {
        path: 'messages',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.cabinetMessages' },
        loadComponent: () =>
          import('./cabinet/cabinet-messages/cabinet-messages.component').then(
            (m) => m.CabinetMessagesComponent,
          ),
      },
      {
        path: 'demandes',
        canActivate: [roleGuard(['PRATICIEN', 'ASSISTANT'])],
        data: { pageTitleKey: 'docTitle.cabinetPending' },
        loadComponent: () =>
          import('./cabinet/assistant-dashboard/assistant-dashboard.component').then(
            (m) => m.AssistantDashboardComponent,
          ),
      },
      { path: 'assistant', pathMatch: 'full', redirectTo: 'demandes' },
      {
        path: 'payments',
        canActivate: [roleGuard(['PRATICIEN', 'ASSISTANT'])],
        data: { pageTitleKey: 'docTitle.cabinetPayments' },
        loadComponent: () =>
          import('./cabinet/cabinet-payments/cabinet-payments.component').then(
            (m) => m.CabinetPaymentsComponent,
          ),
      },

      {
        path: 'profile',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.practitionerProfile' },
        loadComponent: () =>
          import('./cabinet/practitioner-profile/practitioner-profile.component').then(
            m => m.PractitionerProfileComponent,
          ),
      },
      {
        path: 'locations',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.locations' },
        loadComponent: () =>
          import('./cabinet/consultation-locations/consultation-locations.component').then(
            m => m.ConsultationLocationsComponent,
          ),
      },
      {
        path: 'diplomas',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.diplomas' },
        loadComponent: () =>
          import('./cabinet/diplomas/diplomas.component').then(m => m.DiplomasComponent),
      },
      {
        path: 'verifications',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.verifications' },
        loadComponent: () =>
          import('./cabinet/verifications/verifications.component').then(m => m.VerificationsComponent),
      },
      {
        path: 'dashboard',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.cabinetDashboard' },
        loadComponent: () =>
          import('./cabinet/management/cabinet-dashboard/cabinet-dashboard.component').then(
            m => m.CabinetDashboardComponent,
          ),
      },
      {
        path: 'staff',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.cabinetStaff' },
        loadComponent: () =>
          import('./cabinet/management/cabinet-staff/cabinet-staff.component').then(
            m => m.CabinetStaffComponent,
          ),
      },
      {
        path: 'practice',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.cabinetPractice' },
        loadComponent: () =>
          import('./cabinet/management/cabinet-practice/cabinet-practice.component').then(
            m => m.CabinetPracticeComponent,
          ),
      },
      {
        path: 'doctors',
        canActivate: [roleGuard(['PRATICIEN'])],
        data: { pageTitleKey: 'docTitle.cabinetDoctors' },
        loadComponent: () =>
          import('./cabinet/management/cabinet-doctors/cabinet-doctors.component').then(
            m => m.CabinetDoctorsComponent,
          ),
      },
      { path: 'admin/users', pathMatch: 'full', redirectTo: 'staff' },
      { path: 'admin/cabinet', pathMatch: 'full', redirectTo: 'practice' },
      { path: 'admin', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },

  {
    path: 'auth',
    children: [
      {
        path: 'login-pro',
        data: { pageTitleKey: 'docTitle.loginPro' },
        loadComponent: () =>
          import('./auth/login-pro/login-pro.component').then(m => m.LoginProComponent),
      },
      {
        path: 'register-cabinet',
        data: { pageTitleKey: 'docTitle.registerCabinet' },
        loadComponent: () =>
          import('./auth/register-cabinet/register-cabinet.component').then(m => m.RegisterCabinetComponent),
      },
      {
        path: 'register-practitioner',
        data: { pageTitleKey: 'docTitle.registerPractitioner' },
        loadComponent: () =>
          import('./auth/register-practitioner/register-practitioner.component').then(
            m => m.RegisterPractitionerComponent,
          ),
      },
      {
        path: 'verify-otp-pro',
        data: { pageTitleKey: 'docTitle.verifyOtpPro' },
        loadComponent: () =>
          import('./auth/verify-otp-pro/verify-otp-pro.component').then(m => m.VerifyOtpProComponent),
      },
      {
        path: 'forgot-password-pro',
        data: { pageTitleKey: 'docTitle.forgotPasswordPro' },
        loadComponent: () =>
          import('./auth/forgot-password-pro/forgot-password-pro.component').then(
            m => m.ForgotPasswordProComponent,
          ),
      },
      {
        path: 'login',
        data: { pageTitleKey: 'docTitle.loginPatient' },
        loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent),
      },
      {
        path: 'register',
        data: { pageTitleKey: 'docTitle.registerPatient' },
        loadComponent: () =>
          import('./auth/register/register.component').then(m => m.RegisterComponent),
      },
      {
        path: 'verify-otp',
        data: { pageTitleKey: 'docTitle.verifyOtp' },
        loadComponent: () =>
          import('./auth/verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent),
      },
      {
        path: 'forgot-password',
        data: { pageTitleKey: 'docTitle.forgotPassword' },
        loadComponent: () =>
          import('./auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
      },
      {
        path: 'reset-password',
        data: { pageTitleKey: 'docTitle.resetPassword' },
        loadComponent: () =>
          import('./auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
      },
    ],
  },

  {
    path: 'patient',
    canActivate: [authGuard, patientEmailVerifiedGuard],
    loadComponent: () =>
      import('./patient/patient-shell.component').then(m => m.PatientShellComponent),
    children: [
      {
        path: 'prendre-rendez-vous',
        data: { pageTitleKey: 'docTitle.patientBooking' },
        loadComponent: () =>
          import('./patient/patient-booking-wizard/patient-booking-wizard.component').then(
            (m) => m.PatientBookingWizardComponent,
          ),
      },
      {
        path: 'dashboard',
        data: { pageTitleKey: 'docTitle.patientDashboard' },
        loadComponent: () =>
          import('./patient/patient-dashboard/patient-dashboard.component').then(
            m => m.PatientDashboardComponent,
          ),
      },
      {
        path: 'messages',
        data: { pageTitleKey: 'docTitle.patientMessages' },
        loadComponent: () =>
          import('./patient/patient-messages/patient-messages.component').then(
            (m) => m.PatientMessagesComponent,
          ),
      },
      {
        path: 'proches',
        data: { pageTitleKey: 'docTitle.patientProches' },
        loadComponent: () =>
          import('./patient/patient-proches/patient-proches.component').then(
            m => m.PatientProchesComponent,
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: '', pathMatch: 'full' },
];
