# B2U-HUB — Microservice IA (FastAPI)

## Démarrage

```bash
cd ai-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

- Swagger : http://localhost:8000/docs
- Health : http://localhost:8000/api/health

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/score` | Score pondéré + explainability |
| `POST /api/v1/match` | Similarité cosine compétences |
| `POST /api/v1/recommend` | Recommandation missions |
| `POST /api/v1/team` | Formation d'équipe équilibrée |
| `POST /api/v1/cv/analyze` | NLP léger analyse CV |
