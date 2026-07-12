from typing import List
from app.schemas import RecommendRequest, RecommendItem, RecommendResponse
from app.services.matching import cosine_skills_similarity, score_to_100


def recommend_missions(req: RecommendRequest) -> RecommendResponse:
    items: List[RecommendItem] = []
    for m in req.missions:
        sim, common = cosine_skills_similarity(req.etudiant_competences, m.competences_requises)
        score = score_to_100(sim)
        raison = (
            f"Matching {score}% — compétences alignées: {', '.join(common)}"
            if common
            else f"Matching {score}% — peu de compétences communes"
        )
        items.append(
            RecommendItem(mission_id=m.id, titre=m.titre, score_matching=score, raison=raison)
        )
    items.sort(key=lambda x: x.score_matching, reverse=True)
    return RecommendResponse(recommandations=items[: req.top_k])
