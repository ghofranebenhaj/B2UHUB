# Propositions IA — B2U-HUB (alignement PFE)

## 1. Agent IA Smart Matching

**Objectif :** Analyser les compétences des étudiants et les besoins des entreprises pour proposer automatiquement les meilleurs profils ou former des équipes équilibrées.

| Composant | Implémentation |
|-----------|----------------|
| Algorithme | Similarité cosine (TF-IDF) sur compétences |
| API | `POST /api/v1/agents/smart-matching` |
| Backend | `GET /api/ai/agents/smart-matching/{etudiantId}` |
| UI | Page **IA & Matching** → bouton « Lancer Smart Matching » |

**Exemple concret :**
> L'étudiante Alice (Java, Angular, PostgreSQL) est **FORTEMENT RECOMMANDÉE** à 92% pour la mission TechCorp « Application B2U-HUB ». L'agent suggère une équipe {Alice, Bob} avec 85% de couverture des compétences mission.

---

## 2. Système de Scoring Automatique

**Objectif :** Modèle IA qui évalue les candidatures selon compétences, projets passés, soft skills et performance antérieure — score objectif et transparent.

| Critère | Poids |
|---------|-------|
| Compétences (cosine + NLP CV) | 35% |
| Projets passés | 25% |
| Soft skills | 20% |
| Performance antérieure | 20% |

| Composant | Implémentation |
|-----------|----------------|
| API | `POST /api/v1/score` + breakdown explainability |
| UI | Fiche mission → Postuler → tableau 4 critères |

**Exemple concret :**
> Candidature Alice : Compétences 88 (35%) + 3 projets (25%) + Soft 85 (20%) + Performance 72 (20%) = **81.4/100**

---

## 3. Agent Collaboration & Innovation

**Objectif :** Agent (heuristique + **LLM optionnel**) qui détecte des opportunités de collaboration entre entreprises, laboratoires et étudiants.

| Composant | Implémentation |
|-----------|----------------|
| Détection | Graphe heuristique (secteurs, labos, compétences rares) |
| LLM | `OPENAI_API_KEY` → enrichissement du rapport |
| API | `POST /api/v1/agents/collaboration` |
| UI | Page **IA & Matching** → « Détecter opportunités » |

**Exemple concret :**
> **Lab IA & Data — ENSIAS × TechCorp** : mission « Application B2U » — consortium PFE entreprise + labo + équipe étudiants (potentiel 85%).

---

## Activer le LLM (optionnel)

```bash
set OPENAI_API_KEY=sk-...
set OPENAI_MODEL=gpt-4o-mini
uvicorn app.main:app --reload --port 8000
```

Sans clé API, l'agent utilise le mode heuristique (suffisant pour la démo PFE).
