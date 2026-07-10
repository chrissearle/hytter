import type { BookingSummary } from '~/types/booking'

export function useBookings(from?: string, to?: string) {
  return useFetch<BookingSummary[]>('/api/bookings', {
    query: { from, to },
    default: () => []
  })
}
