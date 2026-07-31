import type { Session } from '~/types/booking'

/**
 * True only on the authenticated -> anonymous edge, which is what the backend
 * reports once a Keycloak session has expired and the cookie has been cleared.
 *
 * Guards against firing on the initial load (no previous value) and on the
 * anonymous -> anonymous case, so a visitor who was never logged in is never
 * told their session ended.
 */
export function sessionJustExpired(
  previous: Session | null | undefined,
  next: Session | null | undefined
): boolean {
  return previous?.authenticated === true && next?.authenticated !== true
}
