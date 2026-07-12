import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Candidature, CandidatureHistorique, CandidatureRequest, CandidatureStatut } from '../../models/candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/candidatures`;

  byMission(missionId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.baseUrl}/mission/${missionId}`);
  }

  postuler(request: CandidatureRequest): Observable<Candidature> {
    return this.http.post<Candidature>(this.baseUrl, request);
  }

  updateStatut(id: number, statut: CandidatureStatut): Observable<Candidature> {
    return this.http.patch<Candidature>(`${this.baseUrl}/${id}/statut`, null, {
      params: new HttpParams().set('statut', statut)
    });
  }

  historique(id: number): Observable<CandidatureHistorique[]> {
    return this.http.get<CandidatureHistorique[]>(`${this.baseUrl}/${id}/historique`);
  }
}
