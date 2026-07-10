import type { BookingSummary } from '~/types/booking'

export const BOOKINGS_CACHE_KEY = 'bookings'

export function useBookings(from?: string, to?: string) {
  return useFetch<BookingSummary[]>('/api/bookings', {
    key: BOOKINGS_CACHE_KEY,
    query: { from, to },
    default: () => []
  })
}
