import type { BookingSummary } from '~/types/booking'

export function useBookings(from?: string, to?: string) {
  const config = useRuntimeConfig()

  return useFetch<BookingSummary[]>('/api/bookings', {
    baseURL: config.public.apiBase,
    query: { from, to },
    default: () => []
  })
}
