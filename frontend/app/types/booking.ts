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

export interface Booking {
  id: number
  name: string
  numberOfPeople: number
  hutId: number
  hutName: string
  arrivalDate: string
  departureDate: string
  adminNotes: string | null
  status: BookingStatus
  createdBy: string | null
}

export interface BookingInput {
  name: string
  numberOfPeople: number
  hutId: number
  arrivalDate: string
  departureDate: string
}

export interface Session {
  authenticated: boolean
  name: string | null
  isAdmin: boolean
}
