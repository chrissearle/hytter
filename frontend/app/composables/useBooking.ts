import type { Booking } from '~/types/booking'

export function bookingCacheKey(id: string | number) {
  return `booking-${id}`
}

export function useBooking(id: string | number) {
  return useFetch<Booking>(`/api/bookings/${id}`, { key: bookingCacheKey(id) })
}
