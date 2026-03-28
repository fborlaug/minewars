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
- [ ] **Step 10d — Frontend deployment.** Add an S3 bucket + CloudFront distribution serving the Vue production build. Configure CloudFront error responses to return `index.html` for SPA client-side routing. Output the CloudFront URL.
- [ ] **Step 11a — CI workflow (build + test).** Add a GitHub Actions workflow (`.github/workflows/ci.yml`) triggered on push/PR. Jobs: build the backend (`mvn package`), run backend tests (`mvn test`), build the frontend (`npm ci && npm run build`), run frontend tests (`npm test`). Fail the pipeline on any failure.
- [ ] **Step 11b — CD workflow (deploy).** Add a deploy workflow (`.github/workflows/deploy.yml`) triggered on push to `main`. Build and push the backend container to ECR. Deploy infrastructure via `cdk deploy`. Sync `frontend/dist/` to S3 and invalidate CloudFront. Use GitHub environment secrets for AWS credentials and environment-specific config (DB endpoint, JWT issuer, CORS origins).

## Phase 6 — Tests

- [ ] **Step 12 — Backend integration tests.** Add `@QuarkusTest` tests for `AuthResource`: register success, register duplicate (409), login success, login bad password (401), and register validation errors (400). Add a test for `HelloResource`: unauthenticated returns 401, authenticated returns 200. Use `rest-assured` (included with `quarkus-junit`). Verify all tests pass with `mvn test`.
- [ ] **Step 13 — Frontend unit tests.** Install Vitest and `@vue/test-utils`. Add tests for the auth store (setAuth, logout, isLoggedIn with valid/expired/malformed tokens). Add component tests for `LoginView` and `RegisterView` (renders form, shows error on failed request, calls store on success). Add `"test": "vitest run"` script to `package.json`. Verify all tests pass with `npm test`.

## Phase 7 — Static Analysis & Vulnerability Scanning

- [ ] **Step 14 — Backend static analysis.** Configure the `maven-checkstyle-plugin` or SpotBugs for code quality checks. Add the OWASP `dependency-check-maven` plugin to scan backend dependencies for known CVEs. Run `mvn verify` and fix any findings.
- [ ] **Step 15 — Frontend linting & audit.** Add ESLint with `@vue/eslint-config-typescript`. Add a `"lint": "eslint ."` script to `package.json`. Run `npm audit` to check for vulnerable dependencies. Fix any findings. Verify `npm run lint` and `npm audit` both pass clean.
- [ ] **Step 16 — CI gate.** Add static analysis and vulnerability checks to the CI/CD pipeline (Phase 5). Backend: `mvn verify` (includes OWASP check). Frontend: `npm run lint && npm audit --audit-level=high`. Fail the build on any critical finding.

## Phase 8 — Observability (Logging, Tracing & Metrics)

- [ ] **Step 17 — Backend observability extensions.** Add `quarkus-opentelemetry` for distributed tracing, `quarkus-micrometer-registry-prometheus` for metrics, and configure structured JSON logging via `quarkus-logging-json`. Update `application.properties` with OTLP exporter endpoint and Prometheus scrape path (`/q/metrics`).
- [ ] **Step 18 — Grafana stack (docker-compose).** Add Grafana, Prometheus, Loki, and Tempo to `docker-compose.yml`. Prometheus scrapes the Quarkus `/q/metrics` endpoint. Tempo receives OTLP traces. Loki collects JSON logs (via Docker log driver or Promtail). Grafana is pre-configured with all three as datasources.
- [ ] **Step 19 — Dashboards & verification.** Create a Grafana dashboard (provisioned as JSON) showing: HTTP request rate/latency (from Prometheus), recent logs (from Loki), and trace lookup (from Tempo). Verify end-to-end: make API calls → see metrics in Grafana, logs searchable in Loki, traces visible in Tempo with span linking.

## Phase 9 — Hexagonal Architecture

- [ ] **Step 20 — Restructure packages.** Move classes into a hexagonal package layout: `domain/` (Player entity, business rules), `application/` (TokenService, future use-case services), `adapter/rest/` (AuthResource, HelloResource, DTOs). Dependencies point inward: adapter → application → domain. Verify the project compiles and all tests still pass.

## Phase 10 — Lobby

- [ ] **Step 21 — Backend lobby.** Create `LobbyResource.java` with `POST /api/lobby/create`, `GET /api/lobby/games` (list open games), `POST /api/lobby/join/{gameId}`. Introduce an in-memory `GameSession` model (id, player1, player2, status: WAITING / IN_PROGRESS / FINISHED).
- [ ] **Step 22 — Frontend lobby page.** Create `LobbyView.vue` showing open games with a "Create Game" button and "Join" buttons. Poll or use SSE for live updates of available games.

## Phase 11 — Your Games

- [ ] **Step 23 — Backend my-games endpoint.** Add `GET /api/lobby/my-games` returning the current player's active and past games with results.
- [ ] **Step 24 — Frontend my-games page.** Create `MyGamesView.vue` showing game history (opponent, result, date).

## Phase 12 — Draw the Board

- [ ] **Step 25 — Board data model (backend).** Create a `Board` class: 2D grid of `Cell` (isMine, isRevealed, adjacentMineCount, revealedBy). Add a `BoardGenerator` service that places N mines randomly and computes adjacency counts.
- [ ] **Step 26 — Board rendering (frontend).** Create `BoardComponent.vue` rendering an N×N grid of `CellComponent.vue` tiles using CSS grid. Cell states: hidden, revealed-number, revealed-bomb. Render a static test board first.

## Phase 13 — Game WebSocket & Real-Time Play

Both players play simultaneously in real-time. There are no turns — each player clicks cells freely, and every reveal is instantly broadcast to both boards.

- [ ] **Step 27 — Backend WebSocket endpoint.** Create `GameWebSocket.java` using `quarkus-websockets-next`. Authenticate via JWT token param on the connection. Client sends `REVEAL {row, col}` → server validates, updates the shared board, and broadcasts `CELL_REVEALED {row, col, value, player}` or `GAME_OVER {winner, reason}` to both players.
- [ ] **Step 28 — Game state machine (backend).** Implement `GameEngine` service with `revealCell(gameId, player, row, col)`. Handle chain-reveal for zero-adjacent cells. Win/loss rules: bomb hit → that player loses immediately; all safe cells revealed → player with the most revealed cells wins.
- [ ] **Step 29 — Frontend WebSocket integration.** Create a `useGameSocket` composable (connect, send reveal, handle incoming messages). Wire into `GameView.vue` — clicking a hidden cell sends `REVEAL`; incoming messages update the reactive board state on both players' screens in real-time.

## Phase 14 — Incremental Game Features (one at a time)

- [ ] **Step 30 — Player info overlay.** Show both player names and live revealed-cell counts on the game screen.
- [ ] **Step 31 — Game timer.** Add a backend-authoritative game clock. Display elapsed time or countdown in `GameView.vue`.
- [ ] **Step 32 — Spectator mode.** Allow other users to watch a live game via a read-only WebSocket subscription.
- [ ] **Step 33 — Rematch / play-again.** After the game is over, offer a "Rematch" button that creates a new game with the same opponent.
- [ ] **Step 34 — Leaderboard.** `GET /api/leaderboard` → top players by wins. `LeaderboardView.vue` to display.
- [ ] **Step 35 — Polish.** Animations on cell reveal, sound effects, responsive CSS, dark/light theme toggle.
