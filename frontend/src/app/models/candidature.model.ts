export type CandidatureStatut =
  | 'EN_ATTENTE'
  | 'PRESELECTIONNEE'
  | 'ENTRETIEN'
  | 'ACCEPTEE'
  | 'REFUSEE';

export interface ScoreBreakdown {
  critere: string;
  note: number;
  poids: number;
  contribution: number;
  detail: string;
}

export interface Candidature {
  id: number;
  missionId: number;
  missionTitre: string;
  etudiantId: number;
  etudiantNom: string;
  statut: CandidatureStatut;
  scoreIA?: number;
  explicationScore?: string;
  scoreBreakdown?: ScoreBreakdown[];
  scoreFromAi?: boolean;
  dateCandidature: string;
}

export interface CandidatureHistorique {
  id: number;
  candidatureId: number;
  ancienStatut: CandidatureStatut;
  nouveauStatut: CandidatureStatut;
  dateChangement: string;
}

export interface CandidatureRequest {
  missionId: number;
  etudiantId: number;
}

export const CANDIDATURE_STATUT_LABELS: Record<CandidatureStatut, string> = {
  EN_ATTENTE: 'En attente',
  PRESELECTIONNEE: 'Présélectionnée',
  ENTRETIEN: 'Entretien',
  ACCEPTEE: 'Acceptée',
  REFUSEE: 'Refusée'
};
