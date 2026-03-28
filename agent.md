# Agent Context — MineWars

> This file is maintained for AI agent continuity. Read this first in every new session.

## Project Overview

**MineWars** is a competitive 2-player real-time minesweeper web game. Both players play simultaneously on a shared board — every cell reveal is broadcast instantly. The first to hit a bomb loses; if all safe cells are revealed, the player with the most revealed cells wins.

## Tech Stack

| Layer    | Technology                          | Notes                         |
|----------|-------------------------------------|-------------------------------|
| Backend  | Quarkus 3.32.4, Java 25, Maven      | REST, JWT auth, Panache, Flyway |
| Frontend | Vue 3, TypeScript, Vite 8, CSS      | Vue Router, Pinia             |
| Database | PostgreSQL 17 (Docker, port 5433)    | Flyway migrations             |
| Infra    | AWS CDK (TypeScript)                | VPC, RDS, ECS Fargate, ALB, S3, CloudFront |
| Auth     | SmallRye JWT (RSA-signed)           | 24h token expiry              |

## Project Structure

```
minewars/
├── .github/workflows/ci.yml      # CI: build + test on push/PR
├── .github/workflows/deploy.yml  # CD: deploy to AWS on push to main
├── PLAN.md
├── README.md
├── agent.md
├── docker-compose.yml             # PostgreSQL 17
├── backend/
│   ├── Dockerfile                 # Multi-stage build (maven → eclipse-temurin:25-jre)
│   ├── .dockerignore
│   ├── pom.xml
│   ├── README.md
│   └── src/main/
│       ├── java/com/frobotics/minewars/
│       │   ├── HelloResource.java    # GET /api/hello
│       │   ├── AuthResource.java     # POST /api/auth/register, /api/auth/login
│       │   ├── AuthRequest.java      # Request record with validation
│       │   ├── AuthResponse.java     # Response record (token + username)
│       │   ├── Errors.java           # WebApplicationException factory
│       │   ├── Player.java           # JPA entity (PanacheEntity)
│       │   ├── RateLimitFilter.java  # Per-IP rate limiter (JAX-RS filter)
│       │   └── TokenService.java     # JWT generation (RSA-signed)
│       └── resources/
│           ├── application.properties
│           ├── privateKey.pem         # RSA private key (signing)
│           ├── publicKey.pem          # RSA public key (verification)
│           └── db/migration/
│               └── V1__create_player_table.sql
├── infra/
│   ├── bin/infra.ts               # CDK app entry point
│   ├── lib/minewars-stack.ts      # VPC, RDS, ECS Fargate + ALB, S3, CloudFront, OIDC, JWT keys
│   ├── cdk.json
│   └── package.json
├── scripts/
│   └── generate-jwt-keys.sh       # One-time: generate RSA keys → Secrets Manager
└── frontend/
    ├── package.json
    ├── README.md
    ├── vite.config.ts
    └── src/
        ├── main.ts
        ├── App.vue
        ├── router/index.ts           # Auth guard (meta.requiresAuth)
        ├── stores/auth.ts            # Pinia auth store (token + username)
        ├── services/api.ts           # API helper
        └── views/
            ├── HomeView.vue
            ├── HelloView.vue          # Protected (requiresAuth)
            ├── LoginView.vue
            └── RegisterView.vue
```

## Dependencies (minimal — add as needed)

### Backend (pom.xml)
- `quarkus-rest` — JAX-RS endpoints
- `quarkus-rest-jackson` — JSON serialization
- `quarkus-hibernate-orm-panache` — JPA entities
- `quarkus-jdbc-postgresql` — PostgreSQL database
- `quarkus-flyway` — database migrations
- `quarkus-smallrye-jwt` — JWT verification
- `quarkus-smallrye-jwt-build` — JWT generation
- `quarkus-smallrye-health` — health checks (/q/health)
- `quarkus-arc` — CDI / dependency injection
- `org.mindrot:jbcrypt` — password hashing
- `quarkus-junit` — testing (test scope)

### Frontend (package.json)
- `vue`, `vue-router`, `pinia` — UI framework + routing + state
- Dev: `vite`, `@vitejs/plugin-vue`, `typescript`, `vue-tsc`, `npm-run-all2`

### Not yet added (add when the plan step requires them)
- `quarkus-websockets-next` — real-time game play (Phase 13)

## Conventions

- **Backend package:** `com.frobotics.minewars`
- **REST base path:** `/api/...`
- **Frontend proxy:** Vite proxies `/api` → `localhost:8080` in dev
- **Game communication:** REST for lobby/auth, WebSockets for in-game real-time play
- **YAGNI:** Only add dependencies when actually needed by the current step

## Current Status

- **Current phase:** Phase 7 — Tests
- **Last completed step:** Step 12 — Rate-limit auth endpoints — Phase 6 complete
- **Next step:** Step 13 — Backend integration tests

## Key Decisions

| Decision     | Choice                   | Reason                                                  |
|--------------|--------------------------|---------------------------------------------------------|
| Dependencies | Minimal / YAGNI          | Add only when a plan step requires them                 |
| Database     | PostgreSQL 17 (Docker)   | Migrated from H2 in Step 8; Flyway manages schema       |
| Auth         | RSA-signed JWT, jBCrypt  | Keys in Secrets Manager (prod), classpath files (dev)   |
| Game model   | Real-time simultaneous   | Both players click freely, no turns                     |
| Deployment   | AWS CDK + GitHub Actions | ECS Fargate (backend), S3 + CloudFront (frontend)       |
| Infra        | No NAT Gateway           | RDS in isolated subnet, saves ~$32/month                |
| ECS          | Public subnet + public IP| Fargate pulls from ECR without NAT Gateway              |
| CloudFront   | Single-domain proxy      | /api/* and /q/* route to ALB, no CORS needed            |
| CI/CD        | GitHub Actions           | CI on push/PR, CD on merge to main                      |


## Session Log

| Date       | Summary                                                                                                                                                                                                                                                                                                            |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-03-21 | Phase 1 complete: restructured into backend/ + frontend/, generated Quarkus skeleton with extensions, scaffolded Vue 3 + TS + Router + Pinia, configured Vite proxy + H2 + CORS.                                                                                                                                   |
| 2026-03-21 | Created PLAN.md with 8 phases / 22 steps. Created agent.md.                                                                                                                                                                                                                                                        |
| 2026-03-21 | Phase 2 complete: verified backend GET /api/hello, created HelloView.vue with fetch, added Vite proxy, confirmed end-to-end connectivity. Cleaned up scaffold boilerplate.                                                                                                                                         |
| 2026-03-21 | Project review & cleanup: removed 6 unused backend deps (hibernate, jackson, security, jwt, websockets, h2), removed pinia from frontend, stripped H2/Hibernate config, bumped Quarkus 3.32.3→3.32.4, surefire 3.5.4→3.5.5, all frontend deps to latest. Fixed quarkus-maven-plugin groupId. YAGNI policy adopted. |
| 2026-03-21 | Phase 3 complete: Player entity (Panache), AuthResource (register + login), JWT generation (RSA, 24h expiry), jBCrypt password hashing, H2 datasource. Frontend: Pinia auth store, LoginView, RegisterView, router auth guard, nav with login/logout. All endpoints verified via curl.                             |
| 2026-03-26 | Phase 4 complete (Steps 8-9): Migrated to PostgreSQL 17 (Docker, port 5433) + Flyway. Replaced quarkus-jdbc-h2 with quarkus-jdbc-postgresql, added quarkus-flyway, docker-compose.yml, V1 migration. Fixed player_SEQ sequence issue (PanacheEntity uses SEQUENCE strategy, not IDENTITY). All endpoints verified.  |
| 2026-03-27 | Housekeeping: added root README.md, updated backend + frontend READMEs, split Step 10 into 10a–10d and Step 11 into 11a–11b in PLAN.md, updated agent.md to match current project state.                                                                                                                           |
| 2026-03-27 | Step 10b complete: created infra/ CDK app (TypeScript). MinewarsStack defines VPC (public + isolated subnets, no NAT), RDS PostgreSQL 17 (db.t4g.micro, single-AZ, Secrets Manager credentials, RemovalPolicy.DESTROY), security group (no ingress yet). Outputs: DbEndpoint, DbSecretArn. Verified with cdk synth. |
| 2026-03-27 | Step 10c complete: added ECS Fargate service (256 CPU, 512 MB, 1 task) behind ALB to MinewarsStack. Container built via fromAsset(backend/). Tasks in public subnets with assignPublicIp. DB credentials from Secrets Manager, JDBC URL from RDS endpoint. RDS SG ingress from backend SG on 5432. Health check /q/health. Output: BackendUrl. |
| 2026-03-28 | Step 10d complete: added S3 bucket (private, DESTROY, autoDeleteObjects) + CloudFront distribution. Two origins: S3 via OAC (default) for static assets, ALB for /api/* and /q/* (no CORS needed). SPA routing via 403/404 -> /index.html. BucketDeployment from frontend/dist with cache invalidation. Output: FrontendUrl. |
| 2026-03-28 | Step 11a complete: added .github/workflows/ci.yml. Backend job: Java 25 (Temurin), Maven caching, PostgreSQL 17 service container (port 5433), ./mvnw package. Frontend job: Node 24, npm caching, npm ci, type-check, build, test. Added placeholder "test" script to package.json. |
| 2026-03-28 | Step 11b complete (Phase 5 done): added .github/workflows/deploy.yml. Triggered by CI workflow_run on main (success only). Sets up Java 25, Node 24, AWS credentials (OIDC). Builds backend (-DskipTests), frontend, installs CDK deps, runs cdk deploy --require-approval never. Secrets: AWS_ROLE_ARN, AWS_REGION. |
| 2026-03-29 | Step 12 complete (Phase 6 done): added RateLimitFilter.java — JAX-RS ContainerRequestFilter with in-memory ConcurrentHashMap per-IP buckets. Register: 100/5min, Login: 50/5min. POST-only, uses X-Forwarded-For. Returns 429 + Retry-After. Stale buckets evicted on access. No new dependencies. Verified with curl. |
