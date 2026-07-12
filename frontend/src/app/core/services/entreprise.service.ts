import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Entreprise } from '../../models/entreprise.model';
import { Mission } from '../../models/mission.model';

const DEFAULT_ENTREPRISE: Entreprise = {
  id: 1,
  nom: 'TechCorp',
  email: 'contact@techcorp.fr',
  secteur: 'IT'
};

@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private readonly http = inject(HttpClient);

  /** Charge d'abord via /missions (fonctionne même si /entreprises est bloqué). */
  list(): Observable<Entreprise[]> {
    return this.listFromMissions().pipe(
      catchError(() => of([DEFAULT_ENTREPRISE])),
      catchError(() => of([DEFAULT_ENTREPRISE]))
    );
  }

  /** Liste complète depuis l'API (après redémarrage backend). */
  listFromApi(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(`${environment.apiUrl}/entreprises`).pipe(
      catchError(() => this.http.get<Entreprise[]>(`${environment.apiUrl}/missions/entreprises`))
    );
  }

  listFromMissions(): Observable<Entreprise[]> {
    return this.http.get<Mission[]>(`${environment.apiUrl}/missions`).pipe(
      map((missions) => {
        const byId = new Map<number, Entreprise>();
        for (const m of missions) {
          if (m.entrepriseId && m.entrepriseNom) {
            byId.set(m.entrepriseId, {
              id: m.entrepriseId,
              nom: m.entrepriseNom,
              email: '',
              secteur: ''
            });
          }
        }
        const list = Array.from(byId.values());
        return list.length > 0 ? list : [DEFAULT_ENTREPRISE];
      })
    );
  }
}
