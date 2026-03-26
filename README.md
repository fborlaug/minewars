# MineWars

A competitive 2-player real-time minesweeper web game. Players log in, enter a lobby, start games, and race to reveal cells. Both players play simultaneously — clicks are broadcast to both boards instantly. The first to hit a bomb loses; if all safe cells are revealed, the player with the most revealed cells wins.

## Tech Stack

| Layer    | Technology                        |
|----------|-----------------------------------|
| Backend  | Quarkus 3.32.4, Java 25, Maven   |
| Frontend | Vue 3, TypeScript, Vite 8        |
| Database | PostgreSQL 17 (Docker)            |
| Auth     | SmallRye JWT (RSA-signed), jBCrypt|

## Project Structure

```
minewars/
├── backend/            # Quarkus REST API
├── frontend/           # Vue 3 SPA
└── docker-compose.yml  # PostgreSQL
```

## Quick Start

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

## More Info

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
- [Development Plan](PLAN.md)

