export type MissionStatut = 'OUVERTE' | 'EN_COURS' | 'CLOTUREE';

export interface Mission {
  id: number;
  titre: string;
  description: string;
  statut: MissionStatut;
  competencesRequises: string[];
  dureeSemaines?: number;
  datePublication: string;
  entrepriseId: number;
  entrepriseNom: string;
}

export interface MissionRequest {
  titre: string;
  description: string;
  competencesRequises: string[];
  dureeSemaines?: number;
  statut?: MissionStatut;
  entrepriseId: number;
}
