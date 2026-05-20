import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Proche } from '../models/proche.model';

@Injectable({ providedIn: 'root' })
export class ProcheService {

  private readonly apiUrl = `${environment.patientApiBaseUrl}/api/proches`;

  constructor(private http: HttpClient) {}

  getMyProches(): Observable<Proche[]> {
    return this.http.get<Proche[]>(this.apiUrl);
  }

  getProche(id: number): Observable<Proche> {
    return this.http.get<Proche>(`${this.apiUrl}/${id}`);
  }

  createProche(proche: Proche): Observable<Proche> {
    return this.http.post<Proche>(this.apiUrl, proche);
  }

  updateProche(id: number, proche: Proche): Observable<Proche> {
    return this.http.put<Proche>(`${this.apiUrl}/${id}`, proche);
  }

  deleteProche(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
