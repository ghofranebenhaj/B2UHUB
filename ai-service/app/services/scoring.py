from app.schemas import ScoreRequest, ScoreResponse, ScoreBreakdown
from app.services.matching import cosine_skills_similarity, score_to_100
from app.services.nlp_cv import boost_from_cv

# Système de Scoring Automatique — aligné proposition B2U-HUB
WEIGHTS = {
    "competences": 0.35,
    "projets_passes": 0.25,
    "soft_skills": 0.20,
    "performance_anterieure": 0.20,
}


def compute_score(req: ScoreRequest) -> ScoreResponse:
    sim, common = cosine_skills_similarity(req.etudiant_competences, req.mission_competences)
    competences_note = score_to_100(sim)
    cv_boost = boost_from_cv(req.cv_texte or "", req.mission_competences)
    competences_note = min(100.0, competences_note + cv_boost)

    nb_projets = req.nb_projets_passes or 0
    if nb_projets == 0 and req.cv_texte:
        nb_projets = min(5, req.cv_texte.lower().count("projet"))

    projets_note = min(100.0, nb_projets * 20.0 + (req.annees_experience or 0) * 5.0)
    soft = float(req.soft_skills or 50)
    perf = min(100.0, max(0.0, float(req.performance_anterieure or 50)))

    breakdown = [
        ScoreBreakdown(
            critere="Compétences",
            note=competences_note,
            poids=WEIGHTS["competences"],
            contribution=round(competences_note * WEIGHTS["competences"], 2),
            detail=f"Similarité cosine + NLP CV. Alignées: {', '.join(common) or 'aucune'}",
        ),
        ScoreBreakdown(
            critere="Projets passés",
            note=projets_note,
            poids=WEIGHTS["projets_passes"],
            contribution=round(projets_note * WEIGHTS["projets_passes"], 2),
            detail=f"{nb_projets} projet(s) réalisé(s) · {req.annees_experience or 0} an(s) expérience",
        ),
        ScoreBreakdown(
            critere="Soft skills",
            note=soft,
            poids=WEIGHTS["soft_skills"],
            contribution=round(soft * WEIGHTS["soft_skills"], 2),
            detail="Communication, adaptabilité, travail d'équipe",
        ),
        ScoreBreakdown(
            critere="Performance antérieure",
            note=perf,
            poids=WEIGHTS["performance_anterieure"],
            contribution=round(perf * WEIGHTS["performance_anterieure"], 2),
            detail="Historique missions / candidatures acceptées sur B2U-HUB",
        ),
    ]

    total = sum(b.contribution for b in breakdown)
    explication = " | ".join(
        f"{b.critere}: {b.note:.1f}/100 ({int(b.poids * 100)}%) → +{b.contribution:.1f}"
        for b in breakdown
    )

    exemple = (
        f"Exemple transparent : compétences ({competences_note:.0f}×35%) + "
        f"projets ({projets_note:.0f}×25%) + soft skills ({soft:.0f}×20%) + "
        f"performance ({perf:.0f}×20%) = {total:.1f}/100."
    )

    return ScoreResponse(
        agent="ScoringAgent",
        score=round(total, 2),
        explication=explication,
        breakdown=breakdown,
        message=f"🤖 Agent Scoring Automatique — Score objectif {total:.1f}/100. {exemple}",
    )
