# Base de données B2U-HUB — PostgreSQL

## Modèle (entités principales)

```
utilisateurs (parent)
├── etudiants          → compétences, CV, performance
├── entreprises        → secteur, missions publiées
│
missions               → statut, compétences requises
├── candidatures       → scoreIA, explication, statut
└── equipes            → membres (N étudiants)

notifications          → liées à utilisateur
```

## Démarrer PostgreSQL seul (Docker)

```bash
cd c:\Users\PC\Desktop\piB2U
docker compose up postgres -d
```

| Paramètre | Valeur |
|-----------|--------|
| Host | `localhost` |
| Port | `5432` |
| Base | `b2uhub` |
| User | `b2uhub` |
| Password | `b2uhub` |

## pgAdmin (interface graphique)

http://localhost:5050

| Champ | Valeur |
|-------|--------|
| Email | `admin@b2uhub.local` |
| Password | `admin` |

Ajouter un serveur : Host `postgres` (dans Docker) ou `host.docker.internal` / `localhost` depuis Windows.

## Backend avec PostgreSQL

```bash
docker compose up postgres -d
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Tout le stack (DB + API + IA + front)

```bash
docker compose up -d
```
