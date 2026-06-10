import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED' | 'PARTIAL';
export type PaymentMethod = 'CASH' | 'CARD' | 'CHECK' | 'TRANSFER' | 'OTHER';

export interface PaymentDTO {
  id: number;
  organizationId: number;
  patientId: number;
  patientName: string;
  amount: number;
  totalAmount?: number;
  disease?: string;
  status: PaymentStatus;
  method: PaymentMethod;
  paymentDate: string; // ISO 8601
  description: string;
  appointmentId?: number;
}

export interface PaymentCreateRequestDTO {
  patientId: number;
  patientName: string;
  amount: number;
  totalAmount?: number;
  disease?: string;
  method: PaymentMethod;
  paymentDate?: string; // ISO 8601
  description: string;
  appointmentId?: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly base = (environment.apiBaseUrl ?? '').replace(/\/$/, '');
  private readonly http = inject(HttpClient);

  getCabinetPayments(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(`${this.base}/api/payments/cabinet`);
  }

  createPayment(request: PaymentCreateRequestDTO): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${this.base}/api/payments`, request);
  }

  updateStatus(id: number, status: PaymentStatus): Observable<PaymentDTO> {
    return this.http.patch<PaymentDTO>(`${this.base}/api/payments/${id}/status`, { status });
  }
}
