import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MissionService } from '../../core/services/mission.service';
import { CandidatureService } from '../../core/services/candidature.service';
import { SessionService } from '../../core/services/session.service';
import { Mission } from '../../models/mission.model';
import {
  CANDIDATURE_STATUT_LABELS,
  Candidature,
  CandidatureHistorique,
  CandidatureStatut
} from '../../models/candidature.model';
import { extractApiError } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-mission-candidatures',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mission-candidatures.component.html',
  styleUrl: './mission-candidatures.component.css'
})
export class MissionCandidaturesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly missionService = inject(MissionService);
  private readonly candidatureService = inject(CandidatureService);
  readonly session = inject(SessionService);

  mission?: Mission;
  candidatures: Candidature[] = [];
  historiques: Record<number, CandidatureHistorique[]> = {};
  expandedHistorique = new Set<number>();
  loadingHistorique = new Set<number>();
  actionError = '';
  loading = true;
  error = '';

  readonly statutLabels = CANDIDATURE_STATUT_LABELS;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.missionService.getById(id).subscribe({
      next: (m) => {
        this.mission = m;
        if (!this.session.canVoirCandidats(m)) {
          this.error = 'Accès réservé à l\'entreprise propriétaire de cette mission.';
          this.loading = false;
          return;
        }
        this.loadCandidatures(id);
      },
      error: () => {
        this.error = 'Mission introuvable.';
        this.loading = false;
      }
    });
  }

  loadCandidatures(missionId: number): void {
    this.candidatureService.byMission(missionId).subscribe({
      next: (data) => {
        this.candidatures = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = extractApiError(err, 'Impossible de charger les candidatures.');
        this.loading = false;
      }
    });
  }

  changerStatut(id: number, statut: CandidatureStatut): void {
    if (!this.mission) return;
    this.actionError = '';
    this.candidatureService.updateStatut(id, statut).subscribe({
      next: () => {
        this.expandedHistorique.delete(id);
        delete this.historiques[id];
        this.loadCandidatures(this.mission!.id);
      },
      error: (err) => {
        this.actionError = extractApiError(err, 'Impossible de mettre à jour le statut.');
      }
    });
  }

  toggleHistorique(candidatureId: number): void {
    if (this.expandedHistorique.has(candidatureId)) {
      this.expandedHistorique.delete(candidatureId);
      return;
    }
    this.expandedHistorique.add(candidatureId);
    if (this.historiques[candidatureId]) {
      return;
    }
    this.loadingHistorique.add(candidatureId);
    this.candidatureService.historique(candidatureId).subscribe({
      next: (data) => {
        this.historiques[candidatureId] = data;
        this.loadingHistorique.delete(candidatureId);
      },
      error: () => {
        this.historiques[candidatureId] = [];
        this.loadingHistorique.delete(candidatureId);
      }
    });
  }

  isHistoriqueExpanded(id: number): boolean {
    return this.expandedHistorique.has(id);
  }

  isHistoriqueLoading(id: number): boolean {
    return this.loadingHistorique.has(id);
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

  actionsDisponibles(statut: CandidatureStatut): { label: string; statut: CandidatureStatut; class: string }[] {
    switch (statut) {
      case 'EN_ATTENTE':
        return [
          { label: 'Présélectionner', statut: 'PRESELECTIONNEE', class: 'btn btn-accent' },
          { label: 'Refuser', statut: 'REFUSEE', class: 'btn btn-refuse' }
        ];
      case 'PRESELECTIONNEE':
        return [
          { label: 'Convocation entretien', statut: 'ENTRETIEN', class: 'btn btn-primary' },
          { label: 'Refuser', statut: 'REFUSEE', class: 'btn btn-refuse' }
        ];
      case 'ENTRETIEN':
        return [
          { label: 'Accepter', statut: 'ACCEPTEE', class: 'btn btn-primary' },
          { label: 'Refuser', statut: 'REFUSEE', class: 'btn btn-refuse' }
        ];
      default:
        return [];
    }
  }
}
