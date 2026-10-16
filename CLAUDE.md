# Hytter — Hut Reservation System

## Overview

A cabin/hut reservation system for family and close friends. Anyone can request a
booking; admins approve or reject requests.

## Huts

- **Huldrebakken** — 3 bedrooms: 2 singles, 1 double
- **Trollhaugen** — 2 beds in the lounge, loft space with several mattresses
- **Tent/hammock** — space for one large tent; hammocks can be spread around; room for several

## Domain Rules

Huts and booking name types are **backend enums** (`domain/Hut.kt`,
`domain/BookingNameType.kt`), not database lookup tables. The enum name is the
stored/wire value; the Bokmål label is the enum's `displayName`. The frontend
gets both from `GET /api/reference` and hard-codes neither.

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

"User" means **holding the `hytter` client role `user` (or `admin`)** — not merely
being signed in. The Keycloak realm (`chrissearle.net`) is shared with other
sites, so an account there grants nothing on its own: `HytterPrincipal.hasAccess`
gates the booking detail and edit endpoints, and `/api/session` reports
`hasAccess` so the GUI can explain the difference rather than just refusing.

Anonymous visitors keep the calendar itself — the blocks, the hut, the booking
name and the approved/requested status — but not the detail behind a block.

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
  ESLint runs Prettier through `eslint-plugin-prettier`, so `pnpm lint` fails on
  formatting differences and `pnpm lint:fix` applies them. `pnpm format:check`
  covers the non-code files and runs in `pr-frontend.yaml`.
- **Husky + lint-staged** pre-commit hook running `pnpm lint:fix` on staged
  `.js`/`.mjs`/`.ts`/`.vue` files and `prettier --write` on staged Markdown,
  JSON, CSS and YAML.

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
frontend, unlike `/api`, `/login`, `/callback` and `/logout`. It also exports
`app_build_info{version}` (from `BuildInfo`), registered as a Prometheus `Info`
metric: a Micrometer gauge with that name would be exported as `app_build`.
Dashboard: "Ktor API + JVM".

The frontend has its own metrics on port 9090 (`frontend/server/plugins/metrics.ts`,
`runtimeConfig.metricsPort`), the same series as the other Nuxt apps:
`app_build_info{version}` (from `NUXT_PUBLIC_IMAGE_TAG`, passed by CI as the
`IMAGE_TAG` build arg) and `http_server_request_duration_seconds{method,route_group,status_code}`.
Proxied requests are `route_group="backend"`, so their timing is the round trip
through the backend. Each 404 is logged as `[404] <method> <path> from <referer>`.
Dashboard: "Nuxt Apps", which counts only the Nuxt pods in the namespace.

Umami analytics: `frontend/app/plugins/umami.client.ts` fetches
`/api/_analytics` (`frontend/server/api/_analytics.get.ts`; under `/api/_` so
the proxy doesn't forward it). That route answers with the frontend's
`NUXT_UMAMI_WEBSITE_ID`, `NUXT_UMAMI_DOMAINS` (JSON array) and `NUXT_UMAMI_SRC`,
set only on the production deployment, and with 204 when they're unset. The
plugin also sends `error-<status>` events. Keep the `scripts.registry.umamiAnalytics`
entry in `nuxt.config.ts` (`trigger: false, proxy: false, bundle: false`):
without it `@nuxt/scripts` bundles Umami Cloud's script and sends to Umami
Cloud. No hostnames or site ids in the code.

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
| `SESSION_MAX_AGE_SECONDS` | Optional. Browser-side cookie lifetime, default 36000 (10h). A backstop only — the authoritative limit is Keycloak's refresh token expiry, enforced server-side. Keep at or above Keycloak's SSO Session Max. |

### Session lifetime

The session cookie holds the user's `sub`, name, roles, the **refresh token**,
and both expiry timestamps — never the access token, which is decoded and
discarded at login. On every authenticated request `TokenRefresher.ensureFresh`
either passes the session through, renews it against Keycloak (re-reading name
and roles from the fresh access token, so **role changes propagate within one
access-token lifetime**), or ends it.

A login can never outlive Keycloak's grant: `refreshExpiresAt` always comes from
`refresh_expires_in` and is never extended locally. Keycloak caps each new value
by the remaining SSO Session Max, so it walks down and the session terminates on
schedule. For the same reason the OAuth scopes must stay `openid profile` —
adding `offline_access` would issue a token that effectively never expires.

### Environment variables the frontend needs (non-dev)

| Variable | Purpose |
| --- | --- |
| `NUXT_BACKEND_URL` | In-cluster service address of the backend, e.g. `http://hytter-backend:8080`. Server-side only — never exposed to the browser. |
