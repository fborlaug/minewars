# MineWars

A competitive 2-player real-time minesweeper web game. Players log in, enter a lobby, start games, and race to reveal cells. Both players play simultaneously — clicks are broadcast to both boards instantly. The first to hit a bomb loses; if all safe cells are revealed, the player with the most revealed cells wins.

## Tech Stack

| Layer    | Technology                        |
|----------|-----------------------------------|
| Backend  | Quarkus 3.32.4, Java 25, Maven   |
| Frontend | Vue 3, TypeScript, Vite 8        |
| Database | PostgreSQL 17 (Docker)            |
| Infra    | AWS CDK (TypeScript)              |
| Auth     | SmallRye JWT (RSA-signed), jBCrypt|

## Project Structure

```
minewars/
├── .github/workflows/  # CI (GitHub Actions)
├── backend/            # Quarkus REST API
├── frontend/           # Vue 3 SPA
├── infra/              # AWS CDK (VPC, RDS, ECS, CloudFront)
└── docker-compose.yml  # PostgreSQL (local dev)
```

## CI

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push and PR:

- **Backend:** builds and tests with `./mvnw package` (Java 25, PostgreSQL 17 service container)
- **Frontend:** runs `npm ci`, type-check, build, and test (Node 24)

## Prerequisites

| Tool       | Version | Install                          |
|------------|---------|----------------------------------|
| Java       | 25      | `brew install openjdk@25`        |
| Maven      | 3.9+    | Included via `./mvnw` wrapper    |
| Node.js    | ≥24     | `brew install node`              |
| Docker     | 24+     | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| AWS CLI    | 2.x     | `brew install awscli`            |
| AWS CDK    | 2.x     | `npm install -g aws-cdk`         |

## Quick Start (Local Dev)

### 1. Start PostgreSQL

```sh
docker compose up -d
```

### 2. Start the backend

```sh
cd backend
./mvnw quarkus:dev
```

Runs on http://localhost:8080.

### 3. Start the frontend

```sh
cd frontend
npm install
npm run dev
```

Runs on http://localhost:5173. API calls are proxied to the backend.

## AWS Infrastructure (CDK)

The `infra/` folder contains an AWS CDK app that provisions the production environment (VPC, RDS PostgreSQL, and — in later steps — ECS Fargate, S3, CloudFront).

### One-time AWS setup

**1. Configure AWS CLI** — either via SSO or static credentials:

```sh
# Option A: SSO (recommended if your org uses IAM Identity Center)
aws configure sso

# Option B: static IAM keys
aws configure
```

**2. Verify access:**

```sh
aws sts get-caller-identity
```

**3. Bootstrap CDK** (one-time per account/region — creates the CDK staging bucket and roles):

```sh
cdk bootstrap aws://<ACCOUNT_ID>/<REGION>
```

Replace `<ACCOUNT_ID>` and `<REGION>` with the values from `get-caller-identity` (e.g. `cdk bootstrap aws://123456789012/eu-west-1`).

### Deploy / tear down

```sh
cd infra
npm install
cdk synth       # preview the CloudFormation template
cdk deploy      # create/update resources
cdk destroy     # tear down everything
```

> **Cost note:** the current stack provisions an RDS `db.t4g.micro` instance (~$12/month). Run `cdk destroy` when not in use to avoid charges.

## More Info

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
- [Development Plan](PLAN.md)

