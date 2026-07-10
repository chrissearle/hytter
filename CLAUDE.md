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

## Monitoring

The backend exposes Prometheus metrics at `GET /metrics` (JVM memory/GC,
CPU, and Ktor HTTP request histograms via Micrometer), plus request-scoped
call logging with an `X-Request-Id`-based call ID for log correlation. Pattern
copied from `src/proxy`'s `Monitoring.kt`. `/metrics` is scraped in-cluster
directly against the backend pod — it is **not** proxied through the
frontend, unlike `/api`, `/login`, `/callback` and `/logout`.

## Auth architecture (Keycloak)

The browser only ever talks to the **frontend**'s origin. The backend has no
ingress of its own in production — the frontend proxies `/api/*`, `/login`,
`/callback` and `/logout` through to the backend over the in-cluster service
address (`server/middleware/proxy.ts`, modeled on
`src/javaBin/cupcake-platform/frosting/server/middleware/proxy.ts`). Because of
this, the backend and frontend are same-origin as far as the browser and
Keycloak's `redirect_uri` validation are concerned — **no CORS configuration is
needed or wanted**; if CORS headers ever seem necessary again, that's a sign
the proxy isn't set up correctly.

Auth itself is a server-side OAuth2 Authorization Code flow (not a token held
by the SPA):

1. Browser hits `/login` on the frontend → proxied to the backend → backend's
   Ktor `oauth` provider redirects the browser to Keycloak.
2. User logs in at Keycloak, which redirects the browser to `/callback` on the
   **frontend's public origin** (proxied through to the backend).
3. Backend exchanges the code for tokens directly with Keycloak, decodes the
   access token's claims (`name`, and client roles under
   `resource_access.<clientId>.roles`), and stores them in an encrypted,
   signed session cookie (`HYTTER_SESSION`).
4. `/api/*` calls are authenticated by validating that session cookie — no
   bearer tokens ever reach the browser.

### Configuring a real Keycloak instance

In the Keycloak admin console, for the realm this app uses:

1. **Create a client** (e.g. `hytter`):
   - Client authentication: **On** (confidential client — the backend holds a
     client secret, since it does the code exchange server-side).
   - Standard flow (Authorization Code): **enabled**.
   - Direct access grants: not needed, can be off.
   - **Valid redirect URIs**: the frontend's public origin + `/callback`, e.g.
     `https://hytter.example.com/callback`. This must be the externally
     reachable frontend URL, never the in-cluster backend address — Keycloak
     redirects the browser here directly.
   - **Valid post logout redirect URIs**: the frontend's public origin, e.g.
     `https://hytter.example.com`.
   - Web origins: same as the redirect URI's origin (only matters if you ever
     call Keycloak directly from the browser, which this app doesn't).
2. **Client roles**: on the client's *Roles* tab, create `user` and `admin`.
   Assign them to users/groups as appropriate — these become the values in
   `resource_access.<clientId>.roles` inside the access token, which the
   backend reads to build the `HytterPrincipal`.
3. **Client scopes / claims**: make sure the `name` claim is included in the
   access token (it's part of the default `profile` scope, so as long as the
   client requests `openid profile` — which it does — this should already be
   present). Verify by decoding a token from Keycloak's token exchange during
   testing.
4. **Client secret**: copy it from the client's *Credentials* tab.

### Environment variables the backend needs (non-dev)

| Variable | Purpose |
| --- | --- |
| `AUTH_DISABLED` | Must be `false`/unset — `true` bypasses Keycloak entirely and runs every request as a fixed admin user, dev-only. |
| `KEYCLOAK_ISSUER` | Realm issuer URL, e.g. `https://keycloak.example.com/realms/hytter`. |
| `KEYCLOAK_CLIENT_ID` | The client created above (e.g. `hytter`). |
| `KEYCLOAK_CLIENT_SECRET` | From the client's *Credentials* tab. |
| `PUBLIC_URL` | The frontend's externally reachable origin, e.g. `https://hytter.example.com` — used to build the `/callback` redirect URI and the post-logout redirect. Must exactly match what's registered in Keycloak. |
| `SESSION_ENCRYPT_KEY` / `SESSION_SIGN_KEY` | Hex-encoded 16-byte keys for the session cookie. Without these, a random key is generated at startup (fine for a single dev instance, but breaks sessions on every restart and won't work across multiple replicas). |

### Environment variables the frontend needs (non-dev)

| Variable | Purpose |
| --- | --- |
| `NUXT_BACKEND_URL` | In-cluster service address of the backend, e.g. `http://hytter-backend:8080`. Server-side only — never exposed to the browser. |
