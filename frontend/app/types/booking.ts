export type BookingStatus = 'OPEN' | 'APPROVED'

export interface BookingSummary {
  id: number
  name: string
  hutId: number
  hutName: string
  arrivalDate: string
  departureDate: string
  status: BookingStatus
}
