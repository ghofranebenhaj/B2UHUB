import re
from typing import List

SKILL_LEXICON = [
    "java", "python", "angular", "react", "spring", "spring boot", "docker",
    "kubernetes", "postgresql", "mysql", "fastapi", "node", "typescript",
    "javascript", "devops", "git", "agile", "scrum", "nlp", "machine learning",
    "ia", "tensorflow", "pandas", "sql", "html", "css", "rest", "api",
    "microservices", "aws", "azure", "communication", "leadership", "travail d'équipe",
]


def extract_skills_from_cv(cv_texte: str, competences_connues: List[str] | None = None) -> List[str]:
    text = (cv_texte or "").lower()
    found = set()
    for skill in SKILL_LEXICON:
        if skill in text:
            found.add(skill.title() if len(skill) > 3 else skill.upper())
    for c in competences_connues or []:
        if c.strip().lower() in text:
            found.add(c.strip())
    return sorted(found)


def extract_keywords(cv_texte: str, limit: int = 8) -> List[str]:
    words = re.findall(r"[a-zA-ZÀ-ÿ]{4,}", (cv_texte or "").lower())
    stop = {"avec", "dans", "pour", "cette", "projets", "expérience", "développement"}
    freq: dict[str, int] = {}
    for w in words:
        if w not in stop:
            freq[w] = freq.get(w, 0) + 1
    return [w for w, _ in sorted(freq.items(), key=lambda x: -x[1])[:limit]]


def boost_from_cv(cv_texte: str, mission_competences: List[str]) -> float:
    if not cv_texte or not mission_competences:
        return 0.0
    detected = {s.lower() for s in extract_skills_from_cv(cv_texte)}
    mission = {s.strip().lower() for s in mission_competences}
    if not mission:
        return 0.0
    overlap = len(detected & mission) / len(mission)
    return round(overlap * 15, 2)  # max +15 points on competences


def analyze_cv(cv_texte: str, competences_connues: List[str], mission_competences: List[str] | None = None) -> tuple:
    skills = extract_skills_from_cv(cv_texte, competences_connues)
    keywords = extract_keywords(cv_texte)
    score = 50.0
    if mission_competences:
        detected = {s.lower() for s in skills}
        mission = {s.strip().lower() for s in mission_competences}
        score = round((len(detected & mission) / len(mission)) * 100, 2) if mission else 50.0
    return skills, keywords, score
