import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiOrientationRequest {
  message: string;
  language: string;
  history?: ChatMessage[];
}

export interface SpecialtyRecommendation {
  id: number;
  code: string;
  label: string;
}

export interface AiOrientationResponse {
  message: string;
  specialties: SpecialtyRecommendation[];
  urgency: string;
  warning: string;
  needMoreInfo?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AiOrientationService {
  private readonly http = inject(HttpClient);

  // Calls patientApiBaseUrl /api/public/ai/orientation
  private readonly apiUrl = `${environment.patientApiBaseUrl}/api/public/ai/orientation`;

  getOrientation(message: string, language: string, history?: ChatMessage[]): Observable<AiOrientationResponse> {
    const payload: AiOrientationRequest = { message, language, history };
    return this.http.post<AiOrientationResponse>(this.apiUrl, payload);
  }
}
