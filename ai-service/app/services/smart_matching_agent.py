from typing import List
from app.schemas import (
    SmartMatchingRequest, SmartMatchingResponse,
    ProfileMatch, TeamSuggestion,
)
from app.services.matching import cosine_skills_similarity, score_to_100
from app.services.team_formation import form_balanced_team
from app.schemas import TeamRequest, CandidatItem


def run_smart_matching_agent(req: SmartMatchingRequest) -> SmartMatchingResponse:
    """Agent IA Smart Matching — analyse compétences vs besoins entreprise."""
    profiles: List[ProfileMatch] = []

    for mission in req.missions:
        sim, common = cosine_skills_similarity(req.etudiant_competences, mission.competences_requises)
        score = score_to_100(sim)
        recommandation = "FORTEMENT RECOMMANDÉ" if score >= 75 else "RECOMMANDÉ" if score >= 50 else "À CONSIDÉRER"
        profiles.append(
            ProfileMatch(
                mission_id=mission.id,
                mission_titre=mission.titre,
                entreprise=mission.entreprise,
                score_matching=score,
                competences_alignees=common,
                recommandation=recommandation,
                justification=(
                    f"L'agent a comparé {len(req.etudiant_competences)} compétences étudiant "
                    f"avec {len(mission.competences_requises)} exigences mission — alignement {score}%."
                ),
            )
        )

    profiles.sort(key=lambda p: p.score_matching, reverse=True)
    top = profiles[:3]

    team_suggestion = None
    if req.candidats_equipe and req.mission_cible_id:
        mission = next((m for m in req.missions if m.id == req.mission_cible_id), None)
        if mission:
            team_req = TeamRequest(
                mission_competences=mission.competences_requises,
                candidats=req.candidats_equipe,
                taille_equipe=req.taille_equipe,
            )
            team = form_balanced_team(team_req)
            team_suggestion = TeamSuggestion(
                mission_id=mission.id,
                membres=[c.nom for c in team.membres],
                couverture=team.couverture_competences,
                explication=team.explication,
            )

    exemple = (
        f"Exemple : pour « {top[0].mission_titre} » chez {top[0].entreprise}, "
        f"le profil atteint {top[0].score_matching}% de correspondance "
        f"({', '.join(top[0].competences_alignees) or 'compétences transverses'})."
    ) if top else "Aucune mission à analyser."

    message_agent = (
        f"🤖 Agent Smart Matching — J'ai analysé {len(req.missions)} mission(s). "
        f"Meilleur match : {top[0].mission_titre} ({top[0].score_matching}%). {exemple}"
    )

    return SmartMatchingResponse(
        agent="SmartMatchingAgent",
        message=message_agent,
        profils_recommandes=top,
        suggestion_equipe=team_suggestion,
        methode="cosine_similarity + greedy_team_balancing",
    )
