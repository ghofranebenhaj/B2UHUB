# B2U-HUB — Fiche technique de démarrage (PC Ghofrane)

Guide pas à pas pour cloner, configurer et lancer le projet en local sous **Windows**.

---

## 1. Prérequis à installer

| Outil | Version minimale | Vérification |
|-------|------------------|--------------|
| **Git** | 2.x | `git --version` |
| **Java JDK** | 17+ | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **Node.js** | 18+ (recommandé 20+) | `node -v` |
| **npm** | 9+ | `npm -v` |
| **Docker Desktop** | récent | `docker --version` |
| **Python** *(optionnel, IA)* | 3.11+ | `python --version` |

> **Option sans Docker :** le backend peut tourner avec H2 en mémoire (profil `dev`, données perdues à l’arrêt).

---

## 2. Récupérer le projet depuis GitHub

```powershell
cd C:\Users\ghofrane\Projects
git clone https://github.com/ghofranebenhaj/B2UHUB.git
cd B2UHUB
```

### Récupérer la branche Sprint 2 (si pas encore mergée dans `main`)

```powershell
git fetch origin
git checkout feat/sprint2-backend-optimisation
```

Ou après merge :

```powershell
git checkout main
git pull origin main
```

---

## 3. Architecture des services

```
Navigateur (4200)
    │
    ▼
Frontend Angular ──► Backend Spring Boot (8080) ──► PostgreSQL (5432)
                            │
                            └──► AI Service FastAPI (8000)
```

| Service | Port | URL |
|---------|------|-----|
| Frontend Angular | 4200 | http://localhost:4200 |
| Backend API | 8080 | http://localhost:8080/api |
| Swagger (doc API) | 8080 | http://localhost:8080/swagger-ui.html |
| API Gateway *(optionnel)* | 8081 | http://localhost:8081/api |
| Microservice IA | 8000 | http://localhost:8000/docs |
| PostgreSQL | 5432 | localhost:5432 |
| pgAdmin | 5050 | http://localhost:5050 |

---

## 4. Mode recommandé — Démarrage manuel (4 terminaux)

### Étape 1 — PostgreSQL (Docker)

**Terminal 1 :**

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB
docker compose up postgres -d
```

Vérifier que PostgreSQL est prêt :

```powershell
docker ps
```

Identifiants par défaut :

| Paramètre | Valeur |
|-----------|--------|
| Host | `localhost` |
| Port | `5432` |
| Base | `b2uhub` |
| User | `b2uhub` |
| Password | `b2uhub` |

---

### Étape 2 — Backend Spring Boot

**Terminal 2 :**

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\backend
$env:SPRING_PROFILES_ACTIVE="postgres"
mvn spring-boot:run
```

Attendre le message : `Started B2uHubApplication`

**Test rapide :**

```powershell
curl http://localhost:8080/api/health
```

Ou ouvrir dans le navigateur : http://localhost:8080/api/health

> Au **1er démarrage**, les données démo sont créées automatiquement (entreprises, étudiants, missions, candidatures).

---

### Étape 3 — Microservice IA *(optionnel mais recommandé pour le scoring)*

**Terminal 3 :**

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\ai-service
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

**Test :** http://localhost:8000/api/health

> Si le service IA n’est pas démarré, le backend fonctionne quand même avec un **fallback local** pour le score des candidatures.

---

### Étape 4 — Frontend Angular

**Terminal 4 :**

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\frontend
npm install
npm start
```

**Application :** http://localhost:4200

Le frontend est configuré pour appeler le backend sur **port 8080** (`frontend/src/environments/environment.ts`).

---

## 5. Mode rapide — H2 sans Docker (démo express)

Un seul terminal pour le backend, sans PostgreSQL :

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\backend
mvn spring-boot:run
```

Puis dans un autre terminal :

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\frontend
npm install
npm start
```

---

## 6. Mode Docker Compose (tout-en-un)

Lance PostgreSQL, backend, IA et frontend :

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Gateway | http://localhost:8081 |
| AI Service | http://localhost:8000 |

> Avec Docker Compose, si le frontend ne répond pas, vérifier `frontend/src/environments/environment.ts` :
> - backend direct → `http://localhost:8080/api`
> - via gateway → `http://localhost:8081/api`

---

## 7. Comptes de démonstration

Mot de passe commun : **`demo123`**

| Rôle | Email |
|------|-------|
| Admin | `admin@b2uhub.local` |
| Entreprise | `contact@techcorp.fr` |
| Étudiant | `alice@univ.fr` |
| Étudiant | `bob@univ.fr` |
| Étudiant | `claire@univ.fr` |

### Connexion API (JWT)

```powershell
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"alice@univ.fr","motDePasse":"demo123"}'
```

Réponse : token JWT + `userId` + `role`.

---

## 8. Vérifications après démarrage

| Test | Commande / URL | Résultat attendu |
|------|----------------|------------------|
| Santé backend | http://localhost:8080/api/health | `{"status":"UP"}` |
| Analytics dashboard | http://localhost:8080/api/analytics/summary | JSON avec KPIs |
| Liste missions | http://localhost:8080/api/missions | Tableau JSON |
| Swagger | http://localhost:8080/swagger-ui.html | Interface OpenAPI |
| Santé IA | http://localhost:8000/api/health | `{"status":"UP"}` |
| Frontend | http://localhost:4200 | Page B2U-HUB |

---

## 9. Lancer les tests backend

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB\backend
mvn test
```

Résultat attendu : **47 tests, 0 échec**.

---

## 10. Variables d'environnement (optionnel)

Copier `.env.example` si besoin :

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB
copy .env.example .env
```

| Variable | Description | Défaut |
|----------|-------------|--------|
| `SPRING_PROFILES_ACTIVE` | `dev` (H2) ou `postgres` | `dev` |
| `DB_HOST` | Hôte PostgreSQL | `localhost` |
| `DB_PORT` | Port PostgreSQL | `5432` |
| `DB_NAME` | Nom de la base | `b2uhub` |
| `DB_USER` / `DB_PASSWORD` | Identifiants DB | `b2uhub` |
| `B2U_AI_SERVICE_BASE_URL` | URL microservice IA | `http://localhost:8000` |
| `B2U_JWT_SECRET` | Clé secrète JWT (prod) | voir `application.yml` |

---

## 11. Dépannage fréquent

### Erreur `http://${b2u.ai-service.base-url} is malformed`

→ Mettre à jour le code (branche `feat/sprint2-backend-optimisation`) ou vérifier que `application.yml` contient `b2u.ai-service.base-url` dans la section commune (hors profil `dev` uniquement).

### Dashboard Analytics : « Impossible de joindre le backend »

1. Vérifier que le backend tourne sur **8080**
2. Vérifier `frontend/src/environments/environment.ts` :
   ```ts
   apiUrl: 'http://localhost:8080/api'
   ```
3. Redémarrer le frontend (`Ctrl+C` puis `npm start`)
4. Rafraîchir le navigateur (`Ctrl+F5`)

### Port déjà utilisé

```powershell
netstat -ano | findstr :8080
taskkill /PID <numero_pid> /F
```

### PostgreSQL : connexion refusée

```powershell
docker compose up postgres -d
docker logs b2uhub-postgres
```

### `npm install` échoue

```powershell
cd frontend
Remove-Item -Recurse -Force node_modules
npm cache clean --force
npm install
```

### Fichiers `angular.json` / `package-lock.json` modifiés sans raison

Ce sont des changements locaux automatiques (CLI Angular / npm). Les annuler :

```powershell
git restore frontend/angular.json frontend/package-lock.json
```

---

## 12. Ordre de démarrage recommandé (résumé)

```
1. docker compose up postgres -d     (ou ignorer si mode H2)
2. backend  → mvn spring-boot:run  (profil postgres ou dev)
3. ai-service → uvicorn ...          (optionnel)
4. frontend → npm start
5. Ouvrir http://localhost:4200
```

---

## 13. Script Windows rapide

Le projet inclut `scripts/start-local.bat` qui démarre backend + gateway :

```powershell
cd C:\Users\ghofrane\Projects\B2UHUB
.\scripts\start-local.bat
```

Puis lancer le frontend manuellement dans un autre terminal.

---

*Document généré pour le PFE B2U-HUB — Sprint 2*
