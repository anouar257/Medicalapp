import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthProService } from '../../services/auth-pro.service';
import { PreferencesService } from '../../services/preferences.service';
import { AgendaService, GlobalPlatformStatsDTO } from '../../services/agenda.service';
import { PractitionerService } from '../../services/practitioner.service';
import { PlatformPatientAdminService } from '../../services/platform-patient-admin.service';
import { MedicalOrganizationDTO, PlatformPatientDTO, ProUserDTO } from '../../models/practitioner.model';
import { AppPreferencesToolbarComponent } from '../../shared/app-preferences-toolbar.component';

export type PlatformDashboardTab = 'cabinets' | 'patients';

/** Tableau de bord réservé à l’administration globale de la plateforme. */
@Component({
  selector: 'app-platform-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, AppPreferencesToolbarComponent],
  templateUrl: './platform-dashboard.component.html',
  styleUrls: ['./platform-dashboard.component.scss'],
})
export class PlatformDashboardComponent implements OnInit {
  readonly prefs = inject(PreferencesService);
  private readonly authPro = inject(AuthProService);
  private readonly agenda = inject(AgendaService);
  private readonly practitioner = inject(PractitionerService);
  private readonly platformPatients = inject(PlatformPatientAdminService);
  private readonly router = inject(Router);

  loading = true;
  loadError = '';

  stats: GlobalPlatformStatsDTO | null = null;
  cabinets: MedicalOrganizationDTO[] = [];
  patients: PlatformPatientDTO[] = [];
  patientsLoading = false;

  activeTab: PlatformDashboardTab = 'cabinets';

  expandedCabinetId: number | null = null;
  staffByCabinetId = new Map<number, ProUserDTO[]>();
  staffLoadingId: number | null = null;
  staffError = '';

  cabinetModalOpen = false;
  cabinetEditTarget: MedicalOrganizationDTO | null = null;
  cabinetEditNom = '';
  cabinetEditAdresse = '';
  cabinetEditSaving = false;
  cabinetEditError = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    forkJoin({
      stats: this.agenda.getGlobalPlatformStats().pipe(
        catchError(() => {
          this.loadError = this.prefs.translate('ADMIN.ERRORS.STATS');
          return of(null);
        }),
      ),
      cabinets: this.practitioner.listPlatformCabinets().pipe(
        catchError(() => {
          if (!this.loadError) {
            this.loadError = this.prefs.translate('ADMIN.ERRORS.CABINETS');
          }
          return of([] as MedicalOrganizationDTO[]);
        }),
      ),
    }).subscribe(({ stats, cabinets }) => {
      this.stats = stats;
      this.cabinets = cabinets;
      this.loading = false;
      this.loadPatients();
    });
  }

  loadPatients(): void {
    this.patientsLoading = true;
    this.platformPatients.listPatients().subscribe({
      next: (list) => {
        this.patients = list ?? [];
        this.patientsLoading = false;
      },
      error: () => {
        this.patientsLoading = false;
        this.loadError = this.prefs.translate('ADMIN.ERRORS.PATIENTS_LOAD');
      },
    });
  }

  togglePatientStatus(p: PlatformPatientDTO): void {
    if (!p.id) return;
    const prev = p.actif;
    this.platformPatients.togglePatientActive(p.id).subscribe({
      next: () => {
        const nextActif = !prev;
        this.patients = this.patients.map((row) =>
          row.id === p.id ? { ...row, actif: nextActif } : row,
        );
      },
      error: () => {
        this.loadError = this.prefs.translate('ADMIN.ERRORS.PATIENT_TOGGLE');
      },
    });
  }

  toggleStaff(cabinetId: number): void {
    if (this.expandedCabinetId === cabinetId) {
      this.expandedCabinetId = null;
      return;
    }
    this.expandedCabinetId = cabinetId;
    this.staffError = '';
    if (this.staffByCabinetId.has(cabinetId)) {
      return;
    }
    this.staffLoadingId = cabinetId;
    this.practitioner.listPlatformCabinetUsers(cabinetId).subscribe({
      next: (users) => {
        this.staffByCabinetId.set(cabinetId, users);
        this.staffLoadingId = null;
      },
      error: () => {
        this.staffLoadingId = null;
        this.staffError = this.prefs.translate('ADMIN.ERRORS.STAFF');
      },
    });
  }

  toggleUserStatus(user: ProUserDTO): void {
    const cid = this.expandedCabinetId;
    if (!user.id || cid == null) return;
    const prev = user.actif;
    this.practitioner.toggleUserActive(user.id).subscribe({
      next: () => {
        const nextActif = !prev;
        const list = this.staffByCabinetId.get(cid);
        if (!list) return;
        this.staffByCabinetId.set(
          cid,
          list.map((u) => (u.id === user.id ? { ...u, actif: nextActif } : u)),
        );
      },
      error: () => {
        this.staffError = this.prefs.translate('ADMIN.ERRORS.USER_TOGGLE');
      },
    });
  }

  openCabinetEdit(c: MedicalOrganizationDTO): void {
    this.cabinetEditTarget = c;
    this.cabinetEditNom = c.nom ?? '';
    this.cabinetEditAdresse = c.adresse ?? '';
    this.cabinetEditError = '';
    this.cabinetModalOpen = true;
  }

  closeCabinetModal(): void {
    this.cabinetModalOpen = false;
    this.cabinetEditTarget = null;
    this.cabinetEditSaving = false;
    this.cabinetEditError = '';
  }

  saveCabinetEdit(): void {
    const c = this.cabinetEditTarget;
    if (!c?.id) return;
    const nom = this.cabinetEditNom.trim();
    if (!nom) {
      this.cabinetEditError = this.prefs.translate('ADMIN.ERRORS.CABINET_NAME_REQUIRED');
      return;
    }
    this.cabinetEditSaving = true;
    this.cabinetEditError = '';
    this.practitioner
      .updatePlatformCabinet(c.id, { nom, adresse: this.cabinetEditAdresse.trim() || undefined })
      .subscribe({
        next: (dto) => {
          this.cabinets = this.cabinets.map((row) => (row.id === dto.id ? { ...row, ...dto } : row));
          this.closeCabinetModal();
        },
        error: () => {
          this.cabinetEditError = this.prefs.translate('ADMIN.ERRORS.CABINET_SAVE');
          this.cabinetEditSaving = false;
        },
      });
  }

  isStaffExpanded(id: number): boolean {
    return this.expandedCabinetId === id;
  }

  staffFor(id: number): ProUserDTO[] {
    return this.staffByCabinetId.get(id) ?? [];
  }

  logout(): void {
    this.authPro.logout();
    this.router.navigate(['/auth/login-pro']);
  }
}
