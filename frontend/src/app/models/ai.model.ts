export interface AiStatus {
  aiServiceUp: boolean;
  aiServiceUrl: string;
  agents?: string[];
}

export interface RecommendItem {
  missionId: number;
  titre: string;
  scoreMatching: number;
  raison: string;
}

export interface AiMatch {
  similarity: number;
  methode: string;
  competencesCommunes: string[];
}

export interface CvAnalysis {
  competences: string[];
  keywords: string[];
  score: number;
  fromAi?: boolean;
}

export interface ProfileMatch {
  missionId: number;
  missionTitre: string;
  entreprise: string;
  scoreMatching: number;
  competencesAlignees: string[];
  recommandation: string;
  justification: string;
}

export interface TeamSuggestion {
  missionId: number;
  membres: string[];
  couverture: number;
  explication: string;
}

export interface SmartMatchingResponse {
  agent: string;
  message: string;
  methode: string;
  profilsRecommandes: ProfileMatch[];
  suggestionEquipe?: TeamSuggestion;
}

export interface CollaborationOpportunity {
  type: string;
  titre: string;
  acteurs: string[];
  thematique: string;
  scorePotentiel: number;
  description: string;
  actionSuggeree: string;
}

export interface CollaborationResponse {
  agent: string;
  message: string;
  methode: string;
  llmActive: boolean;
  opportunites: CollaborationOpportunity[];
}

export interface Equipe {
  id: number;
  nom: string;
  missionId: number;
  missionTitre: string;
  dateFormation: string;
  couvertureCompetences?: number;
  explication?: string;
  membresNoms: string[];
}

export interface EtudiantItem {
  id: number;
  nom: string;
  email: string;
  filiere: string;
  competences: string[];
}

export interface NotificationMessage {
  titre: string;
  message: string;
  utilisateurId: number;
}
