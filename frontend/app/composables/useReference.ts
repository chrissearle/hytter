import type { Reference } from '~/types/booking'

export const REFERENCE_CACHE_KEY = 'reference'

export function useReference() {
  return useFetch<Reference>('/api/reference', { key: REFERENCE_CACHE_KEY })
}
