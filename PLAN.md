# MineWars — 2-Player Real-Time Minesweeper

A competitive 2-player minesweeper web game. **Backend**: Quarkus 3.32.4, Java 25, REST + WebSockets, Maven, PostgreSQL. **Frontend**: Vue 3, TypeScript, Vite, HTML5/CSS. Players log in, enter a lobby, start games, and race in real-time to reveal cells. Both players play simultaneously — clicks are broadcast to both boards instantly. The first to hit a bomb loses; if all safe cells are revealed, the player with the most revealed cells wins.

---

## Phase 1 — Project Skeleton

- [x] **Step 1 — Restructure into two folders.** Move the existing Maven project into `backend/` and scaffold a new Vue 3 + TypeScript + Vite project in `frontend/`. Remove or replace the root `pom.xml`.
- [x] **Step 2 — Backend skeleton.** In `backend/`, generate a Quarkus 3.32.4 project (`com.frobotics:minewars-backend`, Java 25). Add extensions: `quarkus-rest`, `quarkus-jdbc-h2`, `quarkus-hibernate-orm-panache`, `quarkus-smallrye-jwt`. Configure `application.properties` with H2 dev defaults.
- [x] **Step 3 — Frontend skeleton.** In `frontend/`, scaffold with `npm create vue@latest` (Vue 3 + TypeScript + Vue Router + Pinia). Configure Vite to proxy `/api` → `localhost:8080` during dev.

## Phase 2 — Hello World (end-to-end smoke test)

- [x] **Step 4 — Backend hello world.** Create `HelloResource.java` exposing `GET /api/hello` returning `"MineWars backend is alive"`. Verify with `mvn quarkus:dev`.
- [x] **Step 5 — Frontend hello world.** Create a `HelloView.vue` that fetches `/api/hello` and displays the response. Verify with `npm run dev` and confirm the proxy works.

## Phase 3 — Authentication (login)

- [x] **Step 6 — Backend auth endpoints.** Create `AuthResource.java` with `POST /api/auth/register` and `POST /api/auth/login`. Add a `Player` JPA entity (id, username, hashed password, wins, losses) persisted to H2. Return a JWT on successful login.
- [x] **Step 7 — Frontend login page.** Create `LoginView.vue` and `RegisterView.vue` with username/password forms. Store JWT in a Pinia auth store. Add a Vue Router guard redirecting unauthenticated users to log in.

## Phase 4 — PostgreSQL

- [x] **Step 8 — Switch to PostgreSQL.** Replace `quarkus-jdbc-h2` with `quarkus-jdbc-postgresql` in `pom.xml`. Add a `docker-compose.yml` at the project root to run PostgreSQL. Update `application.properties` with PostgreSQL connection config (use Quarkus dev-services or explicit Docker container). Remove `drop-and-create` schema strategy. Verify register + login still work against PostgreSQL.
- [x] **Step 9 — Flyway migrations.** Add the `quarkus-flyway` extension to `pom.xml`. Let Flyway own all schema changes (remove any Hibernate schema-management strategy). Create `src/main/resources/db/migration/V1__create_player_table.sql` with the `Player` table DDL. Verify the migration runs on startup and register + login still work.

## Phase 5 — AWS CDK Deployment

- [x] **Step 10a — Backend container image.** Add the `quarkus-smallrye-health` extension to `pom.xml`. Create a production `Dockerfile` that builds the Quarkus app and runs `quarkus-run.jar`. Verify locally with `docker build` and `docker run` (connecting to the local PostgreSQL).
- [x] **Step 10b — CDK project + networking.** Run `cdk init app --language typescript` in a new `infra/` folder. Define a single stack containing: a VPC with public and private isolated subnets (no NAT Gateway — RDS doesn't need outbound internet); an RDS PostgreSQL 17 instance (`db.t4g.micro`, single-AZ) in the isolated subnet with credentials auto-generated into Secrets Manager; a security group for RDS with no ingress rules yet (Step 10c will add ingress from the backend service). Set `RemovalPolicy.DESTROY` and `deleteAutomatedBackups: true` on RDS for easy teardown. Output the RDS endpoint and secret ARN. Verify with `cdk synth`.
- [x] **Step 10c — Backend deployment.** In `MinewarsStack`, add an ECS Fargate service using `ApplicationLoadBalancedFargateService`: build the container image from `../backend` via `ContainerImage.fromAsset`; place tasks in public subnets with `assignPublicIp: true` (avoids NAT Gateway costs); set `cpu: 256`, `memoryLimitMiB: 512`, `desiredCount: 1`. Pass DB credentials from the RDS Secrets Manager secret as container secrets (`QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`). Set `MP_JWT_VERIFY_ISSUER=minewars` and `MINEWARS_JWT_EXPIRY=PT24H` as environment variables. Add an ingress rule on the RDS security group allowing port 5432 from the Fargate service security group. Configure the ALB health check on `/q/health`. Output the ALB URL. Verify with `cdk synth`.
- [x] **Step 10d — Frontend deployment.** Build the frontend (`npm run build` in `frontend/`). In `MinewarsStack`, add a private S3 bucket (`removalPolicy: DESTROY`, `autoDeleteObjects: true`) with a `BucketDeployment` from `../frontend/dist`. Add a CloudFront distribution with two origins: the S3 bucket (default, via OAC) for static assets, and the ALB as a second origin for `/api/*` requests (eliminates CORS — single domain). Configure a custom error response returning `/index.html` with 200 for SPA client-side routing. Output the CloudFront URL. Verify with `cdk synth`.
- [x] **Step 11a — CI workflow (build + test).** Add `.github/workflows/ci.yml` triggered on push and PR. Two jobs: **backend** — set up Java 25 (Temurin) with Maven caching, start a PostgreSQL 17 service container, run `./mvnw package` (compiles, runs tests, packages) in `backend/`; **frontend** — set up Node 22 with npm caching, run `npm ci`, `npm run type-check`, `npm run build`, and `npm test` in `frontend/`. Add a placeholder `"test": "echo No tests yet"` script to `package.json` so the CI passes before Phase 7 adds real tests. Both jobs use `working-directory`. Fail the pipeline on any failure.
- [x] **Step 11b — CD workflow (deploy).** Add `.github/workflows/deploy.yml` triggered on push to `main` after CI passes (`workflow_run` on the CI workflow, branch `main`, conclusion `success`). Single job: set up Java 25 (Temurin), Node 24, and AWS credentials (`aws-actions/configure-aws-credentials` with OIDC or access-key secrets). Build the backend (`./mvnw package -DskipTests` in `backend/` — tests already passed in CI) and the frontend (`npm ci && npm run build` in `frontend/`). Install CDK dependencies (`npm ci` in `infra/`) and run `cdk deploy --require-approval never`. This single command builds the Docker image, pushes to ECR, updates the ECS service, syncs the frontend to S3, and invalidates CloudFront — all in one step. Store AWS account ID, region, and credentials as GitHub repository secrets.

## Phase 6 — Rate Limiting

- [x] **Step 12 — Rate-limit auth endpoints.** Add a `RateLimitFilter.java` JAX-RS `ContainerRequestFilter` with an in-memory `ConcurrentHashMap<String, TokenBucket>` to rate-limit by client IP (from `X-Forwarded-For` behind ALB/CloudFront). Limit `POST /api/auth/register` to 100 requests per IP per 5 minutes. Limit `POST /api/auth/login` to 50 requests per IP per 5 minutes. Return HTTP `429 Too Many Requests` with a `Retry-After` header when exceeded. No new dependencies — pure Java. Works for single-instance (`desiredCount: 1`). Verify with repeated curl requests.

## Phase 7 — Tests

- [ ] **Step 13 — Backend integration tests.** Create `@QuarkusTest` tests under `src/test/java/com/frobotics/minewars/`. Tests run against the PostgreSQL service container in CI (port 5433). Flyway runs the migration automatically. Use `@TestTransaction` for rollback-per-test isolation. No new dependencies — `quarkus-junit` already provides REST Assured.

  **`AuthResourceTest.java`** — all auth paths:
  1. `POST /api/auth/register` valid credentials → 201, `{"message":"Registered"}`
  2. Register same username twice → 409, `"Username taken"`
  3. Register blank username → 400, `"Username and password required"`
  4. Register 2-char username → 400, `"Username must be 3–32 characters"`
  5. Register 7-char password → 400, `"Password must be 8–72 characters"`
  6. Register null body → 400
  7. `POST /api/auth/login` correct credentials → 200, body contains `token` and `username`
  8. Login wrong password → 401, `"Invalid credentials"`
  9. Login non-existent user → 401, `"Invalid credentials"` (no user enumeration)
  10. Login blank fields → 400

  **`HelloResourceTest.java`** — authenticated endpoint:
  1. `GET /api/hello` no auth header → 401
  2. `GET /api/hello` with valid JWT (via `@TestSecurity`) → 200

  **`RateLimitFilterTest.java`** — rate limiting:
  1. Send 51 login requests from same IP → 51st returns 429 with `Retry-After` header
  2. Requests to non-rate-limited path → never 429

  Reset `RateLimitFilter` state between tests to prevent interference (add a `@VisibleForTesting` reset method or clear the bucket map). Verify all tests pass with `./mvnw verify`.
- [ ] **Step 14 — Frontend unit tests.** Install Vitest and `@vue/test-utils`. Add tests for the auth store (setAuth, logout, isLoggedIn with valid/expired/malformed tokens). Add component tests for `LoginView` and `RegisterView` (renders form, shows error on failed request, calls store on success). Add `"test": "vitest run"` script to `package.json`. Verify all tests pass with `npm test`.

## Phase 8 — Static Analysis & Vulnerability Scanning

- [ ] **Step 15 — Backend static analysis.** Configure the `maven-checkstyle-plugin` or SpotBugs for code quality checks. Add the OWASP `dependency-check-maven` plugin to scan backend dependencies for known CVEs. Run `mvn verify` and fix any findings.
- [ ] **Step 16 — Frontend linting & audit.** Add ESLint with `@vue/eslint-config-typescript`. Add a `"lint": "eslint ."` script to `package.json`. Run `npm audit` to check for vulnerable dependencies. Fix any findings. Verify `npm run lint` and `npm audit` both pass clean.
- [ ] **Step 17 — CI gate.** Add static analysis and vulnerability checks to the CI/CD pipeline (Phase 5). Backend: `mvn verify` (includes OWASP check). Frontend: `npm run lint && npm audit --audit-level=high`. Fail the build on any critical finding.

## Phase 9 — Observability (Logging, Tracing & Metrics)

- [ ] **Step 18 — Backend observability extensions.** Add `quarkus-opentelemetry` for distributed tracing, `quarkus-micrometer-registry-prometheus` for metrics, and configure structured JSON logging via `quarkus-logging-json`. Update `application.properties` with OTLP exporter endpoint and Prometheus scrape path (`/q/metrics`).
- [ ] **Step 19 — Grafana stack (docker-compose).** Add Grafana, Prometheus, Loki, and Tempo to `docker-compose.yml`. Prometheus scrapes the Quarkus `/q/metrics` endpoint. Tempo receives OTLP traces. Loki collects JSON logs (via Docker log driver or Promtail). Grafana is pre-configured with all three as datasources.
- [ ] **Step 20 — Dashboards & verification.** Create a Grafana dashboard (provisioned as JSON) showing: HTTP request rate/latency (from Prometheus), recent logs (from Loki), and trace lookup (from Tempo). Verify end-to-end: make API calls → see metrics in Grafana, logs searchable in Loki, traces visible in Tempo with span linking.

## Phase 10 — Hexagonal Architecture

- [ ] **Step 21 — Restructure packages.** Move classes into a hexagonal package layout: `domain/` (Player entity, business rules), `application/` (TokenService, future use-case services), `adapter/rest/` (AuthResource, HelloResource, DTOs). Dependencies point inward: adapter → application → domain. Verify the project compiles and all tests still pass.

## Phase 11 — Lobby

- [ ] **Step 22 — Backend lobby.** Create `LobbyResource.java` with `POST /api/lobby/create`, `GET /api/lobby/games` (list open games), `POST /api/lobby/join/{gameId}`. Introduce an in-memory `GameSession` model (id, player1, player2, status: WAITING / IN_PROGRESS / FINISHED).
- [ ] **Step 23 — Frontend lobby page.** Create `LobbyView.vue` showing open games with a "Create Game" button and "Join" buttons. Poll or use SSE for live updates of available games.

## Phase 12 — Your Games

- [ ] **Step 24 — Backend my-games endpoint.** Add `GET /api/lobby/my-games` returning the current player's active and past games with results.
- [ ] **Step 25 — Frontend my-games page.** Create `MyGamesView.vue` showing game history (opponent, result, date).

## Phase 13 — Draw the Board

- [ ] **Step 26 — Board data model (backend).** Create a `Board` class: 2D grid of `Cell` (isMine, isRevealed, adjacentMineCount, revealedBy). Add a `BoardGenerator` service that places N mines randomly and computes adjacency counts.
- [ ] **Step 27 — Board rendering (frontend).** Create `BoardComponent.vue` rendering an N×N grid of `CellComponent.vue` tiles using CSS grid. Cell states: hidden, revealed-number, revealed-bomb. Render a static test board first.

## Phase 14 — Game WebSocket & Real-Time Play

Both players play simultaneously in real-time. There are no turns — each player clicks cells freely, and every reveal is instantly broadcast to both boards.

- [ ] **Step 28 — Backend WebSocket endpoint.** Create `GameWebSocket.java` using `quarkus-websockets-next`. Authenticate via JWT token param on the connection. Client sends `REVEAL {row, col}` → server validates, updates the shared board, and broadcasts `CELL_REVEALED {row, col, value, player}` or `GAME_OVER {winner, reason}` to both players.
- [ ] **Step 29 — Game state machine (backend).** Implement `GameEngine` service with `revealCell(gameId, player, row, col)`. Handle chain-reveal for zero-adjacent cells. Win/loss rules: bomb hit → that player loses immediately; all safe cells revealed → player with the most revealed cells wins.
- [ ] **Step 30 — Frontend WebSocket integration.** Create a `useGameSocket` composable (connect, send reveal, handle incoming messages). Wire into `GameView.vue` — clicking a hidden cell sends `REVEAL`; incoming messages update the reactive board state on both players' screens in real-time.

## Phase 15 — Incremental Game Features (one at a time)

- [ ] **Step 31 — Player info overlay.** Show both player names and live revealed-cell counts on the game screen.
- [ ] **Step 32 — Game timer.** Add a backend-authoritative game clock. Display elapsed time or countdown in `GameView.vue`.
- [ ] **Step 33 — Spectator mode.** Allow other users to watch a live game via a read-only WebSocket subscription.
- [ ] **Step 34 — Rematch / play-again.** After the game is over, offer a "Rematch" button that creates a new game with the same opponent.
- [ ] **Step 35 — Leaderboard.** `GET /api/leaderboard` → top players by wins. `LeaderboardView.vue` to display.
- [ ] **Step 36 — Polish.** Animations on cell reveal, sound effects, responsive CSS, dark/light theme toggle.
