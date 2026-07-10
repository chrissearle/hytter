import type { Booking } from '~/types/booking'

export function useBooking(id: string | number) {
  return useFetch<Booking>(`/api/bookings/${id}`)
}
