# frontend

MineWars frontend — Vue 3 + TypeScript + Vite.

## Prerequisites

The backend must be running on `http://localhost:8080`. Vite proxies all `/api` requests to it automatically (see `vite.config.ts`).

## Tech Stack

| Library    | Purpose              |
|------------|----------------------|
| Vue 3      | UI framework         |
| TypeScript | Type safety          |
| Vite 8     | Dev server & bundler |
| Vue Router | Client-side routing  |
| Pinia      | State management     |

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

Runs on `http://localhost:5173`. API calls to `/api/*` are proxied to the backend.

### Type-Check, Compile and Minify for Production

```sh
npm run build
```
