# Hytter

Bookingsystem for hyttene — familie og nære venner kan sende inn ønsker, admin
godkjenner.

**Live:** https://hytter.chrissearle.net

## Struktur

| Katalog     | Innhold                                     |
| ----------- | ------------------------------------------- |
| `backend/`  | Kotlin + Ktor + Gradle, Postgres via Flyway |
| `frontend/` | Nuxt 4 + @nuxt/ui, pnpm                     |

## Lokal utvikling

Kopier `local.env.example` til `local.env` og fyll ut. Ingenting laster fila
automatisk — eksporter den selv (eller sett variablene i IDE-et).

```bash
docker compose up -d                      # Postgres på :5433

set -a && source local.env && set +a      # miljøvariabler for backend
cd backend && ./gradlew run               # backend på :8080

cd frontend && pnpm install && pnpm dev   # frontend på :3000
```

Se [CLAUDE.md](CLAUDE.md) for domeneregler, auth-arkitektur og
miljøvariabler.
