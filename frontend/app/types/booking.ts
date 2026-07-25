export type BookingStatus = 'OPEN' | 'APPROVED'

export type Hut = 'HULDREBAKKEN' | 'TROLLHAUGEN' | 'TENT_HAMMOCK'

export type BookingNameType = 'OPPHAVET' | 'SORKISRAMPEN' | 'HA12' | 'PERSONAL' | 'OTHER'

/**
 * Reference data served by GET /api/reference. Hut names and booking groups
 * live in the backend enums, so the GUI never hard-codes either the values or
 * their Bokmål labels.
 */
export interface HutItem {
  value: Hut
  displayName: string
}

export interface NameTypeItem {
  value: BookingNameType
  displayName: string
  isFreeText: boolean
}

export interface Reference {
  huts: HutItem[]
  nameTypes: NameTypeItem[]
}

export interface BookingSummary {
  id: number
  name: string
  hut: Hut
  arrivalDate: string
  departureDate: string
  status: BookingStatus
}

export interface Booking {
  id: number
  nameType: BookingNameType
  name: string
  numberOfPeople: number
  hut: Hut
  arrivalDate: string
  departureDate: string
  adminNotes: string | null
  status: BookingStatus
  /** Display name of the requester — shown to admins, never used for authorization. */
  createdBy: string | null
  /** Resolved by the backend against the session; the owner's identity never reaches the client. */
  canEdit: boolean
}

export interface BookingInput {
  nameType: BookingNameType
  name?: string
  numberOfPeople: number
  hut: Hut
  arrivalDate: string
  departureDate: string
}

export interface Session {
  authenticated: boolean
  name: string | null
  isAdmin: boolean
  /**
   * Logged in, but granted no `hytter` client role. The Keycloak realm is shared
   * with other sites, so an account there does not by itself grant access.
   */
  hasAccess: boolean
  /**
   * The single fixed group this user may book under, or null for admins (who may
   * book any group) and users with no group assigned. Drives the name dropdown;
   * the backend enforces the rule regardless of what the client sends.
   */
  group: BookingNameType | null
}
