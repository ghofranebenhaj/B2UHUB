# B2U-HUB — Business-to-University Hub

Plateforme PFE mettant en relation entreprises et étudiants freelances.

## Structure

```
piB2U/
├── backend/     # Spring Boot 3 — API REST (port 8080)
├── frontend/    # Angular 17 — Interface web (port 4200)
├── ai-service/  # FastAPI — agents IA (port 8000)
└── database/    # Scripts PostgreSQL + doc schéma
```

## Prérequis

- **Java 17+**
- **Maven 3.8+**
- **Node.js 18+** et **npm**
- **Docker** (recommandé pour PostgreSQL)
- *(Optionnel)* PostgreSQL installé localement

## Base de données PostgreSQL

### Option A — Docker (recommandé)

```bash
docker compose up postgres -d
```

| Service | URL / accès |
|---------|-------------|
| PostgreSQL | `localhost:5432` — base `b2uhub` / user `b2uhub` / pass `b2uhub` |
| pgAdmin | http://localhost:5050 — `admin@b2uhub.local` / `admin` |

Puis lancer le backend :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Hibernate crée les tables automatiquement ; les **données démo** sont insérées au 1er démarrage.

### Option B — H2 en mémoire (sans Docker)

```bash
cd backend
mvn spring-boot:run
```

Profil `dev` par défaut — données perdues à l’arrêt.

---

## Démarrage rapide

### 1. Backend (Spring Boot)

- API : http://localhost:8080/api/health
- H2 Console (profil `dev` uniquement) : http://localhost:8080/h2-console

### 2. Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

- App : http://localhost:4200

## API principales

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/missions` | Liste + filtres (`statut`, `competence`, `titre`) |
| POST | `/api/missions` | Créer une mission |
| PATCH | `/api/missions/{id}/statut` | Changer le statut |
| POST | `/api/candidatures` | Postuler (+ score IA) |
| PATCH | `/api/candidatures/{id}/statut` | Accepter / Refuser |
| GET | `/api/analytics/summary` | KPIs dashboard |

## Modules couverts

- **Module 1** : Missions (CRUD, filtres, cycle de vie)
- **Module 2** : Candidatures (postulation, workflow, score IA local ou microservice)
- **Module 3** : Analytics (KPIs + graphiques Chart.js)

## Profils Spring

| Profil | Base | Usage |
|--------|------|--------|
| `dev` (défaut) | H2 mémoire | Dev rapide sans Docker |
| `postgres` | PostgreSQL | **PFE / prod locale** |
| `prod` | PostgreSQL | `ddl-auto: validate` |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Microservice IA (FastAPI) — port 8000

```bash
cd ai-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

| Endpoint | Fonction |
|----------|----------|
| `POST /api/v1/score` | Score 40/30/20/10 + explainability |
| `POST /api/v1/match` | Similarité cosine compétences |
| `POST /api/v1/recommend` | Recommandation missions |
| `POST /api/v1/team` | Formation d'équipe équilibrée |
| `POST /api/v1/cv/analyze` | NLP léger (CV) |

Swagger : http://localhost:8000/docs

## Fonctions avancées (UI)

- **IA & Matching** (`/ia`) : recommandations, matching, analyse CV
- **Fiche mission** : preview matching, score détaillé, former équipe IA
- **WebSocket** : notifications temps réel (STOMP `/ws`)
- **API** : `/api/ai/*`, `/api/equipes/*`

## Docker Compose (optionnel)

```bash
docker compose up --build
```

## Lancer les 3 services (dev local)

1. `uvicorn` dans `ai-service` (port 8000)
2. `mvn spring-boot:run` dans `backend` (port 8080)
3. `npm start` dans `frontend` (port 4200)

## Prochaines étapes

- Authentification JWT complète
- CI/CD GitHub Actions + SonarQube
- Kubernetes / Prometheus / Grafana
