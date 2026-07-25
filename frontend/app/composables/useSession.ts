import type { Session } from '~/types/booking'

export const SESSION_CACHE_KEY = 'session'

/**
 * Shares one fetch across every component that asks (header, booking form,
 * detail page) via the explicit key.
 *
 * The cookie has to be forwarded by hand during SSR: Nuxt does not pass the
 * incoming request's headers to server-side fetches, so without this the server
 * would render every visitor as logged out and the header would flip on
 * hydration.
 */
export function useSession() {
  const headers = import.meta.server ? useRequestHeaders(['cookie']) : undefined

  return useFetch<Session>('/api/session', { key: SESSION_CACHE_KEY, headers })
}
