import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AiService } from '../../core/services/ai.service';
import { EtudiantService } from '../../core/services/etudiant.service';
import { MissionService } from '../../core/services/mission.service';
import {
  AiStatus,
  CollaborationResponse,
  SmartMatchingResponse
} from '../../models/ai.model';
import { Mission } from '../../models/mission.model';

@Component({
  selector: 'app-ia-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ia-hub.component.html',
  styleUrl: './ia-hub.component.css'
})
export class IaHubComponent implements OnInit {
  private readonly aiService = inject(AiService);
  private readonly etudiantService = inject(EtudiantService);
  private readonly missionService = inject(MissionService);

  status?: AiStatus;
  missions: Mission[] = [];
  selectedEtudiantId = 1;
  selectedMissionId?: number;

  smartMatching?: SmartMatchingResponse;
  collaboration?: CollaborationResponse;
  loading = true;

  ngOnInit(): void {
    this.aiService.status().subscribe({
      next: (s) => (this.status = s),
      error: () =>
        (this.status = {
          aiServiceUp: false,
          aiServiceUrl: 'http://localhost:8000',
          agents: []
        })
    });

    this.etudiantService.list().subscribe({
      next: (e) => {
        if (e.length) this.selectedEtudiantId = e[0].id;
      }
    });

    this.missionService.list({ statut: 'OUVERTE' }).subscribe({
      next: (m) => {
        this.missions = m;
        if (m.length) this.selectedMissionId = m[0].id;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  runSmartMatching(): void {
    this.aiService.smartMatching(this.selectedEtudiantId, this.selectedMissionId).subscribe({
      next: (r) => (this.smartMatching = r)
    });
  }

  runCollaboration(): void {
    this.aiService.collaboration().subscribe({
      next: (r) => (this.collaboration = r)
    });
  }
}
