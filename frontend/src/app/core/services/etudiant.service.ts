import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EtudiantItem } from '../../models/ai.model';

@Injectable({ providedIn: 'root' })
export class EtudiantService {
  private readonly http = inject(HttpClient);

  list(): Observable<EtudiantItem[]> {
    return this.http.get<EtudiantItem[]>(`${environment.apiUrl}/etudiants`);
  }
}
