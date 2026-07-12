import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Mission, MissionRequest, MissionStatut } from '../../models/mission.model';

@Injectable({ providedIn: 'root' })
export class MissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/missions`;

  list(filters?: {
    statut?: MissionStatut;
    competence?: string;
    titre?: string;
  }): Observable<Mission[]> {
    let params = new HttpParams();
    if (filters?.statut) params = params.set('statut', filters.statut);
    if (filters?.competence) params = params.set('competence', filters.competence);
    if (filters?.titre) params = params.set('titre', filters.titre);
    return this.http.get<Mission[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Mission> {
    return this.http.get<Mission>(`${this.baseUrl}/${id}`);
  }

  create(request: MissionRequest): Observable<Mission> {
    return this.http.post<Mission>(this.baseUrl, request);
  }

  update(id: number, request: MissionRequest): Observable<Mission> {
    return this.http.put<Mission>(`${this.baseUrl}/${id}`, request);
  }

  updateStatut(id: number, statut: MissionStatut): Observable<Mission> {
    return this.http.patch<Mission>(`${this.baseUrl}/${id}/statut`, null, {
      params: { statut }
    });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  countCandidatures(missionId: number): Observable<number> {
    return this.http.get<{ count: number }>(`${this.baseUrl}/${missionId}/candidatures/count`).pipe(
      map((r) => r.count)
    );
  }
}
