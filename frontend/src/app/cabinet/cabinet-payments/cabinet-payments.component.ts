import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService, PaymentDTO, PaymentCreateRequestDTO } from '../../services/payment.service';
import { PreferencesService } from '../../services/preferences.service';
import { finalize, forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface PaymentRow {
  id?: number;
  appointmentId?: number;
  paymentDate: string;
  patientId: number;
  patientName: string;
  disease: string;
  totalAmount: number;
  amount: number;
  method: string;
}

@Component({
  selector: 'app-cabinet-payments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cabinet-payments.component.html',
  styleUrls: ['./cabinet-payments.component.scss']
})
export class CabinetPaymentsComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly fb = inject(FormBuilder);
  readonly prefs = inject(PreferencesService);
  private readonly http = inject(HttpClient);

  payments: PaymentRow[] = [];
  loading = false;
  
  showModal = false;
  creating = false;

  form = this.fb.group({
    patientId: ['', Validators.required],
    patientName: ['', Validators.required],
    disease: [''],
    totalAmount: [300, [Validators.required, Validators.min(0)]],
    amount: [300, [Validators.required, Validators.min(0)]],
    method: ['CASH', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.loading = true;
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = today.toISOString();
    const endObj = new Date(today);
    endObj.setDate(endObj.getDate() + 1);
    const end = endObj.toISOString();

    const baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

    forkJoin({
      paymentsList: this.paymentService.getCabinetPayments(),
      appointments: this.http.get<any[]>(`${baseUrl}/api/appointments`, { params: { start, end } })
    })
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: ({ paymentsList, appointments }) => {
          const validAppointments = appointments.filter(a => a.status === 'CONFIRMED' || a.status === 'COMPLETED');
          const rows: PaymentRow[] = [];
          const usedPaymentIds = new Set<number>();

          for (const app of validAppointments) {
            if (!app.patientId) continue;
            
            const payment = paymentsList.find(p => p.patientId === app.patientId);
            if (payment) {
              rows.push({
                ...payment,
                disease: payment.disease || app.typeLabel || '',
                totalAmount: payment.totalAmount || 0,
                appointmentId: app.id,
              });
              usedPaymentIds.add(payment.id!);
            } else {
              // Create pending payment row from appointment
              rows.push({
                paymentDate: app.startTime,
                patientId: app.patientId,
                patientName: app.title.includes('—') ? app.title.split('—')[1]?.trim() : (app.patientPrenom ? `${app.patientPrenom} ${app.patientNom}` : app.title),
                disease: app.typeLabel,
                totalAmount: 300,
                amount: 0,
                method: '-',
                appointmentId: app.id
              });
            }
          }

          for (const p of paymentsList) {
            if (!p.id || !usedPaymentIds.has(p.id)) {
              rows.push({
                ...p,
                disease: p.disease || '',
                totalAmount: p.totalAmount || 0,
              });
            }
          }

          rows.sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime());
          this.payments = rows;
        },
        error: (err) => console.error('Failed to load payments', err)
      });
  }

  openModal(prefill?: PaymentRow): void {
    if (prefill) {
      this.form.patchValue({
        patientId: prefill.patientId.toString(),
        patientName: prefill.patientName,
        disease: prefill.disease,
        totalAmount: prefill.totalAmount,
        amount: prefill.totalAmount,
        method: prefill.method === '-' ? 'CASH' : prefill.method,
        description: ''
      });
    } else {
      this.form.reset({ method: 'CASH', totalAmount: 300, amount: 300 });
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const req: PaymentCreateRequestDTO = {
      patientId: Number(val.patientId),
      patientName: val.patientName ?? '',
      amount: Number(val.amount),
      totalAmount: Number(val.totalAmount),
      disease: val.disease ?? '',
      method: val.method as any,
      description: val.description ?? '',
      paymentDate: new Date().toISOString()
    };

    this.creating = true;
    this.paymentService.createPayment(req)
      .pipe(finalize(() => this.creating = false))
      .subscribe({
        next: (res) => {
          this.loadPayments(); // Reload to refresh list and matches
          this.closeModal();
        },
        error: (err) => console.error('Failed to create payment', err)
      });
  }

  getPaymentStatus(p: PaymentRow): { label: string, cssClass: string } {
    const total = p.totalAmount || 0;
    const paid = p.amount || 0;
    if (total === 0) return { label: 'Non défini', cssClass: 'text-gray-500 bg-gray-50 ring-gray-500/10' };
    if (paid >= total) return { label: 'Payé ✅', cssClass: 'text-green-700 bg-green-50 ring-green-600/20' };
    if (paid > 0 && paid < total) return { label: `Partiel ⏳ (${paid}/${total} MAD)`, cssClass: 'text-orange-700 bg-orange-50 ring-orange-600/20' };
    return { label: 'Non payé ❌', cssClass: 'text-red-700 bg-red-50 ring-red-600/10' };
  }
}
