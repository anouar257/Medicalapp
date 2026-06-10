import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/patient.model';
import {
  AgendaService,
  AppointmentPatientDTO,
  AppointmentStatus,
  DoctorReviewRequestDTO,
} from '../../services/agenda.service';
import { PreferencesService } from '../../services/preferences.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.scss'],
})
export class PatientDashboardComponent implements OnInit {
  patient: AuthResponse | null = null;

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly agendaService = inject(AgendaService);
  readonly prefs = inject(PreferencesService);

  appointments: AppointmentPatientDTO[] = [];
  appointmentsLoading = false;
  appointmentsError = '';

  activeTab: 'upcoming' | 'past' | 'all' = 'upcoming';
  currentPage = 1;
  readonly pageSize = 6;

  readonly appointmentSkeletons = [1, 2, 3, 4] as const;

  reviewDialogOpen = false;
  selectedReviewAppointment: AppointmentPatientDTO | null = null;
  reviewRating = 5;
  reviewPunctuality = 5;
  reviewComment = '';
  reviewSubmitting = false;
  reviewError = '';
  reviewSuccess = '';

  ngOnInit() {
    this.patient = this.authService.getCurrentPatient();
    if (!this.patient) this.router.navigate(['/auth/login']);
    else this.loadAppointments();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  private loadAppointments(): void {
    if (!this.patient) return;
    this.appointmentsLoading = true;
    this.appointmentsError = '';

    this.agendaService.getAppointmentsForPatient(this.patient.patientId).subscribe({
      next: (list) => {
        this.appointments = list;
        this.appointmentsLoading = false;
        
        // Choisir l'onglet par défaut intelligemment
        const hasUpcoming = list.some(apt => this.isUpcoming(apt));
        this.activeTab = hasUpcoming ? 'upcoming' : 'all';
        this.currentPage = 1;
      },
      error: (e) => {
        this.appointmentsLoading = false;
        this.appointmentsError = e?.error?.error || this.prefs.translate('Impossible de charger vos rendez-vous.');
      },
    });
  }

  isUpcoming(apt: AppointmentPatientDTO): boolean {
    if (apt.status === 'CANCELLED' || apt.status === 'COMPLETED' || apt.status === 'NO_SHOW') {
      return false;
    }
    const aptTime = new Date(apt.startTime).getTime();
    const now = new Date().getTime();
    return aptTime >= now;
  }

  get filteredAppointments(): AppointmentPatientDTO[] {
    if (this.activeTab === 'all') {
      return [...this.appointments].sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
    }
    if (this.activeTab === 'upcoming') {
      return this.appointments
        .filter(apt => this.isUpcoming(apt))
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    }
    if (this.activeTab === 'past') {
      return this.appointments
        .filter(apt => !this.isUpcoming(apt))
        .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
    }
    return [];
  }

  get paginatedAppointments(): AppointmentPatientDTO[] {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredAppointments.slice(start, end);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredAppointments.length / this.pageSize) || 1;
  }

  get showingStart(): number {
    if (this.filteredAppointments.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get showingEnd(): number {
    const end = this.currentPage * this.pageSize;
    return end > this.filteredAppointments.length ? this.filteredAppointments.length : end;
  }

  changeTab(tab: 'upcoming' | 'past' | 'all') {
    this.activeTab = tab;
    this.currentPage = 1;
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  getDayOfWeek(dateString: string): string {
    const date = new Date(dateString);
    const day = date.toLocaleDateString(this.prefs.language(), { weekday: 'short' });
    return day.replace(/\./g, '').substring(0, 3).toUpperCase();
  }

  getDayOfMonth(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString(this.prefs.language(), { day: '2-digit' });
  }

  getMonthLabel(dateString: string): string {
    const date = new Date(dateString);
    const month = date.toLocaleDateString(this.prefs.language(), { month: 'short' });
    return month.replace(/\./g, '').substring(0, 3).toUpperCase();
  }

  getYearLabel(dateString: string): string | null {
    const date = new Date(dateString);
    const currentYear = new Date().getFullYear();
    if (date.getFullYear() !== currentYear) {
      return String(date.getFullYear());
    }
    return null;
  }

  getTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleTimeString(this.prefs.language(), { hour: '2-digit', minute: '2-digit' });
  }

  statusLabel(status: AppointmentStatus | undefined): string {
    if (status === 'CONFIRMED') return this.prefs.translate('Confirmé');
    if (status === 'PENDING') return this.prefs.translate('En attente');
    if (status === 'CANCELLED') return this.prefs.translate('Annulé');
    return this.prefs.translate('Statut');
  }

  statusBadgeClasses(status: AppointmentStatus | undefined): string {
    if (status === 'CONFIRMED') return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300 border-emerald-200/80 dark:border-emerald-800/50';
    if (status === 'PENDING') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 border-amber-200/80 dark:border-amber-800/50';
    if (status === 'CANCELLED') return 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300 border-rose-200/80 dark:border-rose-800/50';
    return 'bg-slate-100 text-slate-700 dark:bg-slate-700/30 dark:text-slate-200 border-slate-200/80 dark:border-slate-600/50';
  }

  canReview(apt: AppointmentPatientDTO): boolean {
    return apt.status === 'COMPLETED';
  }

  openReview(apt: AppointmentPatientDTO): void {
    this.selectedReviewAppointment = apt;
    this.reviewRating = 5;
    this.reviewPunctuality = 5;
    this.reviewComment = '';
    this.reviewError = '';
    this.reviewSuccess = '';
    this.reviewDialogOpen = true;
  }

  closeReview(): void {
    this.reviewDialogOpen = false;
    this.selectedReviewAppointment = null;
    this.reviewError = '';
  }

  submitReview(): void {
    if (!this.selectedReviewAppointment) {
      return;
    }
    this.reviewError = '';
    this.reviewSuccess = '';
    this.reviewSubmitting = true;

    const body: DoctorReviewRequestDTO = {
      appointmentId: this.selectedReviewAppointment.id,
      rating: this.reviewRating,
      punctualityRating: this.reviewPunctuality,
      comment: this.reviewComment.trim(),
    };

    this.agendaService.createDoctorReview(body).subscribe({
      next: () => {
        this.reviewSubmitting = false;
        this.reviewSuccess = this.prefs.translate('Avis enregistré avec succès.');
        this.closeReview();
        setTimeout(() => (this.reviewSuccess = ''), 3000);
      },
      error: (e) => {
        this.reviewSubmitting = false;
        this.reviewError = e?.error?.error || this.prefs.translate('Impossible d’enregistrer l’avis.');
      },
    });
  }
}
