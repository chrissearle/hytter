import type { MaybeRefOrGetter } from 'vue'
import type { BookingSummary } from '~/types/booking'

export const BOOKINGS_CACHE_KEY = 'bookings'

/**
 * `from`/`to` accept refs or getters so the calendar re-fetches when the user
 * moves between seasons. The cache key stays fixed: mutation pages invalidate
 * by this exact literal via `clearNuxtData`, so a per-season key would leave
 * stale bookings behind after a create or edit.
 */
export function useBookings(from?: MaybeRefOrGetter<string>, to?: MaybeRefOrGetter<string>) {
  return useFetch<BookingSummary[]>('/api/bookings', {
    key: BOOKINGS_CACHE_KEY,
    query: computed(() => ({ from: toValue(from), to: toValue(to) })),
    default: () => []
  })
}
