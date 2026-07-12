from pydantic import BaseModel, Field
from typing import List, Optional


class ScoreRequest(BaseModel):
    etudiant_competences: List[str] = Field(default_factory=list)
    mission_competences: List[str] = Field(default_factory=list)
    annees_experience: Optional[int] = 0
    nb_projets_passes: Optional[int] = 0
    performance_anterieure: Optional[float] = 50.0
    soft_skills: Optional[int] = 50
    disponibilite: Optional[int] = 10
    cv_texte: Optional[str] = ""


class ScoreBreakdown(BaseModel):
    critere: str
    note: float
    poids: float
    contribution: float
    detail: str


class ScoreResponse(BaseModel):
    agent: str = "ScoringAgent"
    score: float
    explication: str
    breakdown: List[ScoreBreakdown]
    message: str = ""


class MatchRequest(BaseModel):
    etudiant_competences: List[str] = Field(default_factory=list)
    mission_competences: List[str] = Field(default_factory=list)


class MatchResponse(BaseModel):
    similarity: float
    methode: str = "cosine_skills"
    competences_communes: List[str]


class MissionItem(BaseModel):
    id: int
    titre: str
    entreprise: str = ""
    competences_requises: List[str] = Field(default_factory=list)


class RecommendRequest(BaseModel):
    etudiant_competences: List[str] = Field(default_factory=list)
    missions: List[MissionItem] = Field(default_factory=list)
    top_k: int = 5


class RecommendItem(BaseModel):
    mission_id: int
    titre: str
    score_matching: float
    raison: str


class RecommendResponse(BaseModel):
    recommandations: List[RecommendItem]


class CandidatItem(BaseModel):
    etudiant_id: int
    nom: str
    competences: List[str] = Field(default_factory=list)
    score_ia: float = 0.0


class TeamRequest(BaseModel):
    mission_competences: List[str] = Field(default_factory=list)
    candidats: List[CandidatItem] = Field(default_factory=list)
    taille_equipe: int = 3


class TeamResponse(BaseModel):
    membres: List[CandidatItem]
    couverture_competences: float
    explication: str


class CvAnalyzeRequest(BaseModel):
    cv_texte: str
    competences_connues: List[str] = Field(default_factory=list)


class CvAnalyzeResponse(BaseModel):
    competences_detectees: List[str]
    mots_cles: List[str]
    score_pertinence: float


# --- Agents IA (propositions B2U-HUB) ---

class ProfileMatch(BaseModel):
    mission_id: int
    mission_titre: str
    entreprise: str
    score_matching: float
    competences_alignees: List[str] = Field(default_factory=list)
    recommandation: str
    justification: str


class TeamSuggestion(BaseModel):
    mission_id: int
    membres: List[str]
    couverture: float
    explication: str


class SmartMatchingRequest(BaseModel):
    etudiant_nom: str = "Étudiant"
    etudiant_competences: List[str] = Field(default_factory=list)
    missions: List[MissionItem] = Field(default_factory=list)
    mission_cible_id: Optional[int] = None
    candidats_equipe: List[CandidatItem] = Field(default_factory=list)
    taille_equipe: int = 3


class SmartMatchingResponse(BaseModel):
    agent: str
    message: str
    profils_recommandes: List[ProfileMatch]
    suggestion_equipe: Optional[TeamSuggestion] = None
    methode: str


class EntrepriseItem(BaseModel):
    nom: str
    secteur: str = ""


class LaboratoireItem(BaseModel):
    nom: str
    domaines: List[str] = Field(default_factory=list)


class EtudiantItem(BaseModel):
    nom: str
    filiere: str = ""
    competences: List[str] = Field(default_factory=list)


class MissionThemeItem(BaseModel):
    id: int
    titre: str
    entreprise: str
    competences_requises: List[str] = Field(default_factory=list)
    statut: str = "OUVERTE"


class CollaborationRequest(BaseModel):
    entreprises: List[EntrepriseItem] = Field(default_factory=list)
    laboratoires: List[LaboratoireItem] = Field(default_factory=list)
    etudiants: List[EtudiantItem] = Field(default_factory=list)
    missions: List[MissionThemeItem] = Field(default_factory=list)
    max_opportunites: int = 5


class CollaborationOpportunity(BaseModel):
    type: str
    titre: str
    acteurs: List[str]
    thematique: str
    score_potentiel: float
    description: str
    action_suggeree: str


class CollaborationResponse(BaseModel):
    agent: str
    message: str
    opportunites: List[CollaborationOpportunity]
    llm_active: bool = False
    methode: str
