# Hytter — Hut Reservation System

## Overview

A cabin/hut reservation system for family and close friends. Anyone can request a
booking; admins approve or reject requests.

## Huts

- **Huldrebakken** — 3 bedrooms: 2 singles, 1 double
- **Trollhaugen** — 2 beds in the lounge, loft space with several mattresses
- **Tent/hammock** — space for one large tent; hammocks can be spread around; room for several

## Domain Rules

A booking (reservation wish) includes:

- **Name** — dropdown: Opphavet, Sørkisrampen, HA12, Personlig, Other (free text,
  max 100 chars). Required so admins know who is requesting.
  - **Personlig** — for when someone is booking just for themselves, not their
    family group. The name value becomes the user's display name (`name` claim
    from Keycloak); anonymous users can edit it as free text instead. Number of
    people still applies as normal (a personal booking can still be for more than
    one person, e.g. a friend tagging along).
- **Number of people**
- **Cabin preference**
- **Arrival / departure dates**
- **Admin notes** — free text, set by admin, visible to the requester (e.g.
  "Du må ha telt/hengekøye de første 2 dager"). Used to flag things like handover
  overlaps that the system itself doesn't manage.

**Status**: `Open` (requested) → `Approved`.

- Admin sets a booking to `Approved`.
- If a user edits an `Approved` booking, it reverts to `Open` (no separate admin
  action needed to trigger this).

**Season**: primary usage is June through end of August — this is the default
range shown in calendar views.

**Overlap handling**: bookings use full-day granularity only; the system does not
detect or prevent overlaps. Same-day handover between huts is common and expected;
longer overlaps (e.g. hammock guests overlapping during a changeover) are also
possible. It's the admin's responsibility to notice and manage overlaps — use the
admin notes field to communicate details to the affected requester(s).

## Roles & Permissions

| Role      | Create booking  | Edit own booking | View status |
| --------- | :-------------: | :--------------: | :---------: |
| Admin     | ✓ (full access) | ✓ (any booking)  |      ✓      |
| User      |        ✓        |        ✓         |      ✓      |
| Anonymous |        ✓        |        ✗         |      ✗      |

## Auth

- Login via **Keycloak OIDC**.
- Client roles (not realm roles) distinguish `user` and `admin`.
- Each user has a `name` claim — this is the source for the name dropdown values
  (see the first three dropdown entries above).

## Views

- **Availability calendar** (site index) — shows booking blocks. All users see the hut name and the booking name field (Opphavet/HA12/osv) per block. Logged in users get links to view details (all) and edit (own). Adnin users get links to edit (all)
- **Booking detail** — click into a block to see full details and status.
- **Booking edit** — edit an existing booking (same form as new booking).
- **New booking** — create a reservation wish.

## Language

GUI text should be in **Bokmål Norwegian**.

## Tech Stack

### Backend

- Kotlin + Ktor + Gradle, latest stable versions of each.
- Follow the standard `api` package pattern from user-level conventions:
  `ApiError.kt`, `HttpStatusCodeSerializer.kt`, `Respond.kt` (package likely
  `no.<org>.hytter.api` or `.plugins.api` if a plugins layer is introduced).
- **Arrow** used broadly — `Either`, `Raise`, `Option`, collection utilities.
- Service methods use `context(_: Raise<ApiError>)`; routes wrap service calls in
  `either { ... }.respond()`. Always `onLeft`/`onRight`, never `ifLeft`/`ifRight`.
- Include a `BuildInfo` object and `/version` endpoint per the standard pattern
  (reads `image-tag.txt` from classpath, falls back to `"development"` locally).
- Must pass **kotlinter** (formatting) and **detekt** (static analysis) before
  finishing any edit session.
- **Renovate** for dependency updates.

### Frontend

- **Nuxt 4.x** with **@nuxt/ui**, custom theming (not the default look) — see
  `frontend-design` skill for aesthetic direction.
- Cabin images embedded in the app (bundled assets, not external storage).
  Placeholder images until real photos are available.
- **pnpm** (never npm), exact package versions.
- **TypeScript** throughout — no `any`; ESLint + Prettier for linting/formatting.
- **Husky + lint-staged** pre-commit hook running `pnpm lint:fix` on staged
  `.js`/`.ts`/`.vue` files.

### Database

- **Postgres**.
- Local dev: docker-compose, configured via `local.env`.
- Production: CNPG (CloudNativePG) on cluster.
