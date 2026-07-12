import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MissionStatut } from '../../models/mission.model';
import { Mission } from '../../models/mission.model';
import { extractApiError } from '../../core/utils/http-error.util';
import { MissionService } from '../../core/services/mission.service';
import { CandidatureService } from '../../core/services/candidature.service';
import { AiService } from '../../core/services/ai.service';
import { SessionService } from '../../core/services/session.service';
import { Candidature, CandidatureStatut } from '../../models/candidature.model';
import { AiMatch, Equipe } from '../../models/ai.model';

@Component({
  selector: 'app-mission-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './mission-detail.component.html',
  styleUrl: './mission-detail.component.css'
})
export class MissionDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly missionService = inject(MissionService);
  private readonly candidatureService = inject(CandidatureService);
  private readonly aiService = inject(AiService);
  readonly session = inject(SessionService);

  mission?: Mission;
  candidatures: Candidature[] = [];
  candidatCount = 0;
  matchPreview?: AiMatch;
  equipe?: Equipe;
  loading = true;
  applying = false;
  formingTeam = false;
  message = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.missionService.getById(id).subscribe({
      next: (m) => {
        this.mission = m;
        this.missionService.countCandidatures(id).subscribe({
          next: (c) => (this.candidatCount = c),
          error: () => (this.candidatCount = 0)
        });
        if (this.session.canVoirCandidats(m)) {
          this.loadCandidatures(id);
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
        this.message = 'Mission introuvable.';
      }
    });
  }

  loadCandidatures(missionId: number): void {
    this.candidatureService.byMission(missionId).subscribe({
      next: (data) => {
        this.candidatures = data;
        this.candidatCount = data.length;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  candidatLabel(): string {
    return this.candidatCount <= 1
      ? `${this.candidatCount} candidat`
      : `${this.candidatCount} candidats`;
  }

  previewMatch(): void {
    if (!this.mission || !this.session.isEtudiant()) return;
    this.aiService.match(this.session.userId, this.mission.id).subscribe({
      next: (m) => (this.matchPreview = m)
    });
  }

  formTeam(): void {
    if (!this.mission || !this.session.canVoirCandidats(this.mission)) return;
    this.formingTeam = true;
    this.aiService.formTeam(this.mission.id, 2).subscribe({
      next: (e) => {
        this.equipe = e;
        this.message = e.explication ?? 'Équipe formée.';
        this.formingTeam = false;
      },
      error: (err) => {
        this.message = extractApiError(err, 'Impossible de former l\'équipe (acceptez des candidatures d\'abord).');
        this.formingTeam = false;
      }
    });
  }

  postuler(): void {
    if (!this.mission || !this.session.canPostuler(this.mission)) return;
    this.applying = true;
    this.message = '';
    this.candidatureService
      .postuler({ missionId: this.mission.id, etudiantId: this.session.userId })
      .subscribe({
        next: () => {
          this.message = 'Candidature envoyée — score IA calculé.';
          this.missionService.countCandidatures(this.mission!.id).subscribe({
            next: (c) => (this.candidatCount = c)
          });
          this.applying = false;
        },
        error: (err) => {
          this.message = extractApiError(err, 'Erreur lors de la candidature.');
          this.applying = false;
        }
      });
  }

  valider(id: number, statut: CandidatureStatut): void {
    this.candidatureService.updateStatut(id, statut).subscribe({
      next: () => this.loadCandidatures(this.mission!.id)
    });
  }

  changerStatut(statut: MissionStatut): void {
    if (!this.mission || !this.session.canVoirCandidats(this.mission)) return;
    this.missionService.updateStatut(this.mission.id, statut).subscribe({
      next: (m) => {
        this.mission = m;
        this.message = 'Statut mis à jour.';
      },
      error: () => (this.message = 'Erreur lors du changement de statut.')
    });
  }

  supprimer(): void {
    if (!this.mission || !this.session.canVoirCandidats(this.mission)) return;
    if (!confirm(`Supprimer « ${this.mission.titre} » ?`)) return;
    this.missionService.delete(this.mission.id).subscribe({
      next: () => this.router.navigate(['/missions']),
      error: () => (this.message = 'Impossible de supprimer la mission.')
    });
  }

  statutClass(statut: CandidatureStatut): string {
    const map: Record<CandidatureStatut, string> = {
      EN_ATTENTE: 'badge-attente',
      PRESELECTIONNEE: 'badge-preselectionnee',
      ENTRETIEN: 'badge-entretien',
      ACCEPTEE: 'badge-acceptee',
      REFUSEE: 'badge-refusee'
    };
    return `badge ${map[statut]}`;
  }
}
