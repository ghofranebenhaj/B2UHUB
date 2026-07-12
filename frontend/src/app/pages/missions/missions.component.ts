import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { MissionService } from '../../core/services/mission.service';
import { SessionService } from '../../core/services/session.service';
import { Mission, MissionStatut } from '../../models/mission.model';
import { extractApiError } from '../../core/utils/http-error.util';

export interface MissionWithCount extends Mission {
  candidatCount: number;
}

@Component({
  selector: 'app-missions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './missions.component.html',
  styleUrl: './missions.component.css'
})
export class MissionsComponent implements OnInit {
  private readonly missionService = inject(MissionService);
  private readonly route = inject(ActivatedRoute);
  readonly session = inject(SessionService);

  missions: MissionWithCount[] = [];
  loading = true;
  error = '';
  success = '';

  filterStatut: MissionStatut | '' = '';
  filterCompetence = '';
  filterTitre = '';

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['ok'] === 'created') {
        this.success = 'Mission créée avec succès.';
      } else if (params['ok'] === 'updated') {
        this.success = 'Mission mise à jour.';
      }
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.missionService
      .list({
        statut: this.filterStatut || undefined,
        competence: this.filterCompetence || undefined,
        titre: this.filterTitre || undefined
      })
      .subscribe({
        next: (data) => {
          if (data.length === 0) {
            this.missions = [];
            this.loading = false;
            return;
          }
          forkJoin(
            data.map((m) =>
              this.missionService.countCandidatures(m.id).pipe(
                map((count) => ({ ...m, candidatCount: count } as MissionWithCount)),
                catchError(() => of({ ...m, candidatCount: 0 } as MissionWithCount))
              )
            )
          ).subscribe({
            next: (withCounts) => {
              this.missions = withCounts;
              this.loading = false;
            },
            error: () => {
              this.missions = data.map((m) => ({ ...m, candidatCount: 0 }));
              this.loading = false;
            }
          });
        },
        error: (err) => {
          this.error = extractApiError(err, 'Impossible de charger les missions.');
          this.loading = false;
        }
      });
  }

  candidatLabel(count: number): string {
    return count <= 1 ? `${count} candidat` : `${count} candidats`;
  }

  statutClass(statut: MissionStatut): string {
    const map: Record<MissionStatut, string> = {
      OUVERTE: 'badge-ouverte',
      EN_COURS: 'badge-en-cours',
      CLOTUREE: 'badge-cloturee'
    };
    return `badge ${map[statut]}`;
  }

  supprimer(id: number, titre: string): void {
    if (!confirm(`Supprimer la mission « ${titre} » ?`)) return;
    this.success = '';
    this.missionService.delete(id).subscribe({
      next: () => {
        this.success = 'Mission supprimée.';
        this.load();
      },
      error: (err) => {
        this.error = extractApiError(err, 'Impossible de supprimer la mission.');
      }
    });
  }
}
