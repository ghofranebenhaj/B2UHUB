-- B2U-HUB — Initialisation PostgreSQL
-- Les tables sont créées par Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- Les données de démo sont insérées par DataInitializer (profil postgres/dev)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

COMMENT ON DATABASE b2uhub IS 'B2U-HUB — Business-to-University Hub (PFE)';
