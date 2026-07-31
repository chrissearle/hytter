/**
 * Revalidates the session on every client-side navigation.
 *
 * Without this, a session that expires mid-visit is never noticed: `useSession`
 * caches under a fixed key and a user who only browses the public calendar
 * never triggers a 401 to react to, so the header would keep claiming they are
 * logged in indefinitely.
 */
export default defineNuxtRouteMiddleware((to, from) => {
  // The server pass already fetches the session as part of rendering.
  if (import.meta.server) return

  // Hydration runs this once with an unchanged route; nothing to revalidate yet.
  if (to.fullPath === from.fullPath) return

  refreshNuxtData(SESSION_CACHE_KEY)
})
