/**
 * Treats a 401 from any API call as "the backend no longer accepts our
 * session", and re-reads /api/session so the UI stops claiming we are logged
 * in. The backend clears the cookie and answers `authenticated: false` once a
 * Keycloak session has expired; the watcher in app.vue turns that into a toast.
 *
 * Wrapping `globalThis.$fetch` catches every useFetch and $fetch call without
 * each composable having to opt in. Client-only on purpose: the server instance
 * is shared across requests, so patching it there would leak between visitors.
 */
export default defineNuxtPlugin((nuxtApp) => {
  const base = globalThis.$fetch

  globalThis.$fetch = base.create({
    onResponseError({ request, response }) {
      if (response.status !== 401) return

      // The session endpoint's own 401 must not re-trigger a session fetch.
      if (String(request).includes('/api/session')) return

      nuxtApp.runWithContext(() => refreshNuxtData(SESSION_CACHE_KEY))
    }
  })
})
