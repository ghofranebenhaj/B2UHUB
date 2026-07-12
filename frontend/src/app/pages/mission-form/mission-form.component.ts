import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MissionService } from '../../core/services/mission.service';
import { EntrepriseService } from '../../core/services/entreprise.service';
import { Entreprise } from '../../models/entreprise.model';
import { MissionStatut } from '../../models/mission.model';
import { extractApiError } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-mission-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mission-form.component.html',
  styleUrl: './mission-form.component.css'
})
export class MissionFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly missionService = inject(MissionService);
  private readonly entrepriseService = inject(EntrepriseService);

  isEdit = false;
  missionId?: number;
  loading = true;
  saving = false;
  error = '';
  entrepriseHint = '';

  entreprises: Entreprise[] = [];

  titre = '';
  description = '';
  competencesText = '';
  dureeSemaines?: number;
  statut: MissionStatut = 'OUVERTE';
  entrepriseId: number | null = null;

  compareIds = (a: number | null, b: number | null): boolean =>
    a != null && b != null && Number(a) === Number(b);

  ngOnInit(): void {
    const path = this.route.snapshot.routeConfig?.path ?? '';
    this.isEdit = path.includes('edit');
    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.isEdit && idParam) {
      this.missionId = Number(idParam);
    }
    this.loadEntreprises();
  }

  loadEntreprises(): void {
    this.loading = true;
    this.error = '';
    this.entrepriseService.list().subscribe({
      next: (data) => this.onEntreprisesLoaded(data, false),
      error: () => {
        this.entrepriseService.listFromMissions().subscribe({
          next: (data) => this.onEntreprisesLoaded(data, true),
          error: (err) => {
            this.error = extractApiError(err, 'Impossible de charger les entreprises.');
            this.loading = false;
          }
        });
      }
    });
  }

  private onEntreprisesLoaded(data: Entreprise[], fallback: boolean): void {
    this.entreprises = data;
    if (fallback) {
      this.entrepriseHint = 'Liste chargée via les missions (redémarrez le backend pour corriger /api/entreprises).';
    }
    if (data.length === 0) {
      this.error = 'Aucune entreprise. Redémarrez le backend : mvn spring-boot:run';
      this.loading = false;
      return;
    }
    if (!this.isEdit && this.entrepriseId == null) {
      this.entrepriseId = Number(data[0].id);
    }
    if (this.isEdit && this.missionId) {
      this.loadMission(this.missionId);
    } else {
      this.loading = false;
    }
  }

  loadMission(id: number): void {
    this.missionService.getById(id).subscribe({
      next: (m) => {
        this.titre = m.titre;
        this.description = m.description;
        this.competencesText = (m.competencesRequises ?? []).join(', ');
        this.dureeSemaines = m.dureeSemaines;
        this.statut = m.statut;
        this.entrepriseId = Number(m.entrepriseId);
        this.loading = false;
      },
      error: (err) => {
        this.error = extractApiError(err, 'Mission introuvable.');
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (!this.titre.trim() || !this.description.trim()) {
      this.error = 'Le titre et la description sont obligatoires.';
      return;
    }
    const eid = Number(this.entrepriseId);
    if (!eid || Number.isNaN(eid)) {
      this.error = 'Sélectionnez une entreprise dans la liste.';
      return;
    }

    const competencesRequises = this.competencesText
      .split(',')
      .map((c) => c.trim())
      .filter((c) => c.length > 0);

    const payload = {
      titre: this.titre.trim(),
      description: this.description.trim(),
      competencesRequises,
      dureeSemaines: this.dureeSemaines ? Number(this.dureeSemaines) : undefined,
      statut: this.statut,
      entrepriseId: eid
    };

    this.saving = true;
    this.error = '';

    const req =
      this.isEdit && this.missionId
        ? this.missionService.update(this.missionId, payload)
        : this.missionService.create(payload);

    req.subscribe({
      next: (m) => {
        this.saving = false;
        this.router.navigate(['/missions'], {
          queryParams: { ok: this.isEdit ? 'updated' : 'created', id: m.id }
        });
      },
      error: (err) => {
        this.error = extractApiError(err, 'Erreur lors de l\'enregistrement.');
        this.saving = false;
      }
    });
  }
}
