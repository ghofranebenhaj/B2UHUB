from typing import List, Set
from app.schemas import TeamRequest, TeamResponse, CandidatItem


def form_balanced_team(req: TeamRequest) -> TeamResponse:
    if not req.candidats:
        return TeamResponse(membres=[], couverture_competences=0.0, explication="Aucun candidat disponible.")

    mission_skills = {s.strip().lower() for s in req.mission_competences if s.strip()}
    sorted_cands = sorted(req.candidats, key=lambda c: c.score_ia, reverse=True)

    selected: List[CandidatItem] = []
    covered: Set[str] = set()

    for cand in sorted_cands:
        if len(selected) >= req.taille_equipe:
            break
        cand_skills = {s.strip().lower() for s in cand.competences}
        new_skills = cand_skills - covered
        # Greedy: prefer candidates adding new mission skill coverage
        if not selected or new_skills & mission_skills or len(selected) < req.taille_equipe:
            selected.append(cand)
            covered |= cand_skills

    coverage = 0.0
    if mission_skills:
        coverage = round(len(covered & mission_skills) / len(mission_skills) * 100, 2)

    noms = ", ".join(c.nom for c in selected)
    explication = (
        f"Équipe de {len(selected)} membres formée (greedy équilibré). "
        f"Couverture compétences mission: {coverage}%. Membres: {noms}."
    )
    return TeamResponse(membres=selected, couverture_competences=coverage, explication=explication)
