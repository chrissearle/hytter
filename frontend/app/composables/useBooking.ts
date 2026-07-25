import type { Booking } from '~/types/booking'

export function bookingCacheKey(id: string | number) {
  return `booking-${id}`
}

/**
 * The detail endpoint requires a login, and the response's `canEdit` is resolved
 * against the caller - so the session cookie has to be forwarded during SSR or
 * the server would render a 401 for everyone. See useSession for the details.
 */
export function useBooking(id: string | number) {
  const headers = import.meta.server ? useRequestHeaders(['cookie']) : undefined

  return useFetch<Booking>(`/api/bookings/${id}`, { key: bookingCacheKey(id), headers })
}
