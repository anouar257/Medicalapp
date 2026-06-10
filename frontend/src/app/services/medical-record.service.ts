import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MedicalRecord {
  appointmentId: number;
  antecedents: string;
  symptomes: string;
  consultation: string;
  diagnostique: string;
  consommable: string;
  acte: string;
  radiologie: string;
}

export interface MedicalRecordDTO {
  appointmentId?: number;
  antecedents?: string;
  symptomes?: string;
  consultation?: string;
  diagnostique?: string;
  consommable?: string;
  acte?: string;
  radiologie?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MedicalRecordService {
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');
  private readonly http = inject(HttpClient);

  getMedicalRecord(appointmentId: number): Observable<MedicalRecordDTO> {
    return this.http.get<MedicalRecordDTO>(`${this.base}/api/appointments/${appointmentId}/medical-record`);
  }

  createMedicalRecord(appointmentId: number, record: MedicalRecordDTO): Observable<MedicalRecordDTO> {
    return this.http.post<MedicalRecordDTO>(`${this.base}/api/appointments/${appointmentId}/medical-record`, record);
  }

  updateMedicalRecord(appointmentId: number, record: MedicalRecordDTO): Observable<MedicalRecordDTO> {
    return this.http.put<MedicalRecordDTO>(`${this.base}/api/appointments/${appointmentId}/medical-record`, record);
  }
}
