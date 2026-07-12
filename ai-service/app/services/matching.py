from typing import List, Tuple
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


def _skills_to_text(skills: List[str]) -> str:
    return " ".join(s.strip().lower() for s in skills if s.strip())


def cosine_skills_similarity(etudiant: List[str], mission: List[str]) -> Tuple[float, List[str]]:
    if not mission:
        return 0.0, []
    etu = {s.strip().lower() for s in etudiant if s.strip()}
    mis = {s.strip().lower() for s in mission if s.strip()}
    common = sorted(etu & mis)
    if not etu and not mis:
        return 0.0, common
    docs = [_skills_to_text(etudiant) or "none", _skills_to_text(mission) or "none"]
    matrix = TfidfVectorizer().fit_transform(docs)
    sim = float(cosine_similarity(matrix[0:1], matrix[1:2])[0][0])
    # Blend exact match ratio for interpretability
    exact_ratio = len(common) / len(mis) if mis else 0.0
    blended = 0.6 * sim + 0.4 * exact_ratio
    return round(min(1.0, blended), 4), common


def score_to_100(similarity: float) -> float:
    return round(similarity * 100, 2)
