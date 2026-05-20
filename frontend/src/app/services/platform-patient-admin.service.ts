import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PlatformPatientDTO {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
  actif: boolean;
  dateInscription: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlatformPatientAdminService {
  private base = `${environment.practitionerApiBaseUrl}/api/pro/platform/patients`;

  constructor(private http: HttpClient) {}

  listPatients(): Observable<PlatformPatientDTO[]> {
    return this.http.get<PlatformPatientDTO[]>(this.base);
  }

  togglePatientActive(patientId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/${patientId}/toggle-active`, {});
  }
}
