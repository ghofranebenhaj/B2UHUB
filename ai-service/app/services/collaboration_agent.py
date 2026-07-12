import os
import json
from typing import List
from app.schemas import (
    CollaborationRequest, CollaborationResponse, CollaborationOpportunity,
)


def _heuristic_opportunities(req: CollaborationRequest) -> List[CollaborationOpportunity]:
    """Détection rule-based des synergies entre entreprises, labos et étudiants."""
    opportunities: List[CollaborationOpportunity] = []
    themes_entreprises: dict[str, list] = {}

    for ent in req.entreprises:
        key = (ent.secteur or "general").lower()
        themes_entreprises.setdefault(key, []).append(ent)

    # Synergie inter-entreprises même secteur
    for secteur, ents in themes_entreprises.items():
        if len(ents) >= 1 and req.missions:
            missions_secteur = [m for m in req.missions if m.entreprise in [e.nom for e in ents]]
            if missions_secteur:
                opportunities.append(
                    CollaborationOpportunity(
                        type="ENTREPRISE_ENTREPRISE",
                        titre=f"Cluster innovation — secteur {secteur.upper()}",
                        acteurs=[e.nom for e in ents],
                        thematique=secteur,
                        score_potentiel=78.0,
                        description=(
                            f"Les entreprises {', '.join(e.nom for e in ents)} partagent le secteur « {secteur} ». "
                            f"L'agent propose un projet conjoint sur {len(missions_secteur)} mission(s) active(s)."
                        ),
                        action_suggeree="Organiser un atelier co-création entreprise + étudiants freelances.",
                    )
                )

    # Laboratoire ↔ Entreprise (expertise complémentaire)
    for lab in req.laboratoires:
        for mission in req.missions:
            overlap = set(lab.domaines) & set(mission.competences_requises)
            if overlap or lab.nom:
                opportunities.append(
                    CollaborationOpportunity(
                        type="LABO_ENTREPRISE",
                        titre=f"R&D {lab.nom} × {mission.entreprise}",
                        acteurs=[lab.nom, mission.entreprise],
                        thematique=", ".join(overlap) if overlap else mission.titre,
                        score_potentiel=85.0 if overlap else 65.0,
                        description=(
                            f"Le laboratoire « {lab.nom} » ({', '.join(lab.domaines)}) peut renforcer "
                            f"la mission « {mission.titre} » via expertise {', '.join(overlap) or 'transverse'}."
                        ),
                        action_suggeree="Montage consortium PFE : entreprise commanditaire + labo encadrant + équipe étudiants.",
                    )
                )

    # Étudiant ↔ Entreprise (multi-compétences rares)
    for etu in req.etudiants:
        rare_skills = [s for s in etu.competences if s.lower() in {"machine learning", "ia", "fastapi", "devops", "kubernetes"}]
        for mission in req.missions:
            if rare_skills and any(s.lower() in [c.lower() for c in mission.competences_requises] for s in etu.competences):
                opportunities.append(
                    CollaborationOpportunity(
                        type="ETUDIANT_ENTREPRISE",
                        titre=f"Talent {etu.nom} → {mission.entreprise}",
                        acteurs=[etu.nom, mission.entreprise],
                        thematique=mission.titre,
                        score_potentiel=82.0,
                        description=(
                            f"{etu.nom} ({etu.filiere}) possède des compétences différenciantes "
                            f"({', '.join(rare_skills[:3])}) alignées avec « {mission.titre} »."
                        ),
                        action_suggeree="Mise en relation directe via B2U-HUB + mission pilote 8-12 semaines.",
                    )
                )

    # Dédupliquer par titre
    seen = set()
    unique = []
    for o in sorted(opportunities, key=lambda x: x.score_potentiel, reverse=True):
        if o.titre not in seen:
            seen.add(o.titre)
            unique.append(o)
    return unique[: req.max_opportunites]


def _llm_enrich(message: str, opportunities: List[CollaborationOpportunity]) -> str:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return message
    try:
        from openai import OpenAI
        client = OpenAI(api_key=api_key)
        summary = json.dumps([o.model_dump() for o in opportunities[:3]], ensure_ascii=False)
        resp = client.chat.completions.create(
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            messages=[
                {"role": "system", "content": "Tu es l'agent Collaboration & Innovation de B2U-HUB. Réponds en français, 3 phrases max."},
                {"role": "user", "content": f"Enrichis ce rapport de collaboration: {summary}"},
            ],
            max_tokens=200,
        )
        return resp.choices[0].message.content or message
    except Exception:
        return message


def run_collaboration_agent(req: CollaborationRequest) -> CollaborationResponse:
    """Agent Collaboration & Innovation — détecte opportunités entre acteurs."""
    opportunities = _heuristic_opportunities(req)

    exemple = (
        f"Exemple détecté : « {opportunities[0].titre} » — "
        f"{opportunities[0].acteurs[0]} pourrait collaborer avec {opportunities[0].acteurs[-1]} "
        f"sur la thématique {opportunities[0].thematique} (potentiel {opportunities[0].score_potentiel}%)."
    ) if opportunities else "Aucune synergie majeure détectée pour l'instant."

    message = (
        f"🤖 Agent Collaboration & Innovation — {len(opportunities)} opportunité(s) identifiée(s) "
        f"en croisant {len(req.entreprises)} entreprise(s), {len(req.laboratoires)} labo(s) "
        f"et {len(req.etudiants)} profil(s) étudiant. {exemple}"
    )

    llm_mode = bool(os.getenv("OPENAI_API_KEY"))
    if llm_mode:
        message = _llm_enrich(message, opportunities)

    return CollaborationResponse(
        agent="CollaborationInnovationAgent",
        message=message,
        opportunites=opportunities,
        llm_active=llm_mode,
        methode="graph_heuristique" + (" + LLM" if llm_mode else ""),
    )
