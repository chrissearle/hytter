import type { Session } from '~/types/booking'

export function useSession() {
  return useFetch<Session>('/api/session')
}
