import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Mission } from '../../models/mission.model';

export type UserRole = 'ETUDIANT' | 'ENTREPRISE';

const STORAGE_KEY = 'b2uhub.session';

interface SessionState {
  role: UserRole;
  userId: number;
}

/** Contexte utilisateur démo (en attendant JWT). */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly state$ = new BehaviorSubject<SessionState>(this.load());

  readonly role$ = new BehaviorSubject<UserRole>(this.state$.value.role);

  get role(): UserRole {
    return this.state$.value.role;
  }

  get userId(): number {
    return this.state$.value.userId;
  }

  setRole(role: UserRole): void {
    const userId = role === 'ETUDIANT' ? 1 : 1;
    this.persist({ role, userId });
  }

  isEtudiant(): boolean {
    return this.role === 'ETUDIANT';
  }

  isEntreprise(): boolean {
    return this.role === 'ENTREPRISE';
  }

  isMissionOwner(mission: Mission): boolean {
    return this.isEntreprise() && Number(mission.entrepriseId) === Number(this.userId);
  }

  canPostuler(mission: Mission): boolean {
    return this.isEtudiant() && mission.statut === 'OUVERTE' && !this.isMissionOwner(mission);
  }

  canVoirCandidats(mission: Mission): boolean {
    return this.isMissionOwner(mission);
  }

  private load(): SessionState {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as SessionState;
        if (parsed.role === 'ETUDIANT' || parsed.role === 'ENTREPRISE') {
          return { role: parsed.role, userId: Number(parsed.userId) || 1 };
        }
      }
    } catch {
      /* ignore */
    }
    return { role: 'ETUDIANT', userId: 1 };
  }

  private persist(state: SessionState): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    this.state$.next(state);
    this.role$.next(state.role);
  }
}
