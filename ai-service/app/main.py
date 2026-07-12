from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.schemas import (
    ScoreRequest, ScoreResponse,
    MatchRequest, MatchResponse,
    RecommendRequest, RecommendResponse,
    TeamRequest, TeamResponse,
    CvAnalyzeRequest, CvAnalyzeResponse,
    SmartMatchingRequest, SmartMatchingResponse,
    CollaborationRequest, CollaborationResponse,
)
from app.services.scoring import compute_score
from app.services.matching import cosine_skills_similarity, score_to_100
from app.services.recommendation import recommend_missions
from app.services.team_formation import form_balanced_team
from app.services.nlp_cv import analyze_cv
from app.services.smart_matching_agent import run_smart_matching_agent
from app.services.collaboration_agent import run_collaboration_agent

app = FastAPI(
    title="B2U-HUB AI Agents",
    description=(
        "3 agents IA B2U-HUB : Smart Matching, Scoring Automatique, "
        "Collaboration & Innovation (LLM optionnel)"
    ),
    version="2.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/health")
def health():
    return {"status": "UP", "service": "b2u-ai"}


@app.post("/api/v1/score", response_model=ScoreResponse)
def score_candidature(req: ScoreRequest):
    return compute_score(req)


@app.post("/api/v1/match", response_model=MatchResponse)
def match_skills(req: MatchRequest):
    sim, common = cosine_skills_similarity(req.etudiant_competences, req.mission_competences)
    return MatchResponse(
        similarity=score_to_100(sim),
        competences_communes=common,
    )


@app.post("/api/v1/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest):
    return recommend_missions(req)


@app.post("/api/v1/team", response_model=TeamResponse)
def form_team(req: TeamRequest):
    return form_balanced_team(req)


@app.post("/api/v1/cv/analyze", response_model=CvAnalyzeResponse)
def analyze_cv_endpoint(req: CvAnalyzeRequest):
    skills, keywords, score = analyze_cv(req.cv_texte, req.competences_connues)
    return CvAnalyzeResponse(
        competences_detectees=skills,
        mots_cles=keywords,
        score_pertinence=score,
    )


@app.post("/api/v1/agents/smart-matching", response_model=SmartMatchingResponse)
def agent_smart_matching(req: SmartMatchingRequest):
    """Agent IA Smart Matching — profils + équipes équilibrées."""
    return run_smart_matching_agent(req)


@app.post("/api/v1/agents/collaboration", response_model=CollaborationResponse)
def agent_collaboration(req: CollaborationRequest):
    """Agent Collaboration & Innovation — opportunités entre acteurs."""
    return run_collaboration_agent(req)
