import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AiMatch,
  AiStatus,
  CollaborationResponse,
  CvAnalysis,
  Equipe,
  RecommendItem,
  SmartMatchingResponse
} from '../../models/ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/ai`;

  status(): Observable<AiStatus> {
    return this.http.get<AiStatus>(`${this.baseUrl}/status`);
  }

  smartMatching(etudiantId: number, missionId?: number): Observable<SmartMatchingResponse> {
    let params = new HttpParams();
    if (missionId) params = params.set('missionId', missionId);
    return this.http.get<SmartMatchingResponse>(
      `${this.baseUrl}/agents/smart-matching/${etudiantId}`,
      { params }
    );
  }

  collaboration(): Observable<CollaborationResponse> {
    return this.http.get<CollaborationResponse>(`${this.baseUrl}/agents/collaboration`);
  }

  recommend(etudiantId: number, topK = 5): Observable<RecommendItem[]> {
    return this.http.get<RecommendItem[]>(`${this.baseUrl}/recommend/${etudiantId}`, {
      params: new HttpParams().set('topK', topK)
    });
  }

  match(etudiantId: number, missionId: number): Observable<AiMatch> {
    return this.http.get<AiMatch>(`${this.baseUrl}/match`, {
      params: { etudiantId, missionId }
    });
  }

  analyzeCv(etudiantId: number): Observable<CvAnalysis> {
    return this.http.get<CvAnalysis>(`${this.baseUrl}/cv/${etudiantId}`);
  }

  formTeam(missionId: number, taille = 3): Observable<Equipe> {
    return this.http.post<Equipe>(`${environment.apiUrl}/equipes/mission/${missionId}`, null, {
      params: new HttpParams().set('taille', taille)
    });
  }
}
