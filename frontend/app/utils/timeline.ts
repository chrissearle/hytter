import type { BookingSummary } from '~/types/booking'

const DAY_MS = 24 * 60 * 60 * 1000

export function toUtcDate(value: string): number {
  const parts = value.split('-').map(Number)
  return Date.UTC(parts[0] ?? 0, (parts[1] ?? 1) - 1, parts[2] ?? 1)
}

export interface TimelineMonth {
  label: string
  monthStart: number
  monthEnd: number
}

/** Splits a season range into calendar months, clamped to the season's start/end. */
export function buildMonths(seasonStart: string, seasonEnd: string): TimelineMonth[] {
  const start = toUtcDate(seasonStart)
  const end = toUtcDate(seasonEnd)

  const result: TimelineMonth[] = []
  const cursor = new Date(start)

  while (Date.UTC(cursor.getUTCFullYear(), cursor.getUTCMonth(), 1) <= end) {
    const year = cursor.getUTCFullYear()
    const month = cursor.getUTCMonth()
    const firstOfMonth = Date.UTC(year, month, 1)
    const lastOfMonth = Date.UTC(year, month + 1, 0)

    result.push({
      label: new Date(firstOfMonth).toLocaleDateString('nb-NO', {
        month: 'long',
        year: 'numeric',
        timeZone: 'UTC'
      }),
      monthStart: Math.max(firstOfMonth, start),
      monthEnd: Math.min(lastOfMonth, end)
    })

    cursor.setUTCMonth(cursor.getUTCMonth() + 1)
  }

  return result
}

export function daysInRange(monthStart: number, monthEnd: number): Date[] {
  const count = Math.round((monthEnd - monthStart) / DAY_MS) + 1
  return Array.from({ length: count }, (_, i) => new Date(monthStart + i * DAY_MS))
}

export function todayIndexInRange(monthStart: number, dayCount: number, now: Date): number {
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
  const index = Math.round((today - monthStart) / DAY_MS)
  return index >= 0 && index < dayCount ? index : -1
}

export interface TimelineBlock {
  booking: BookingSummary
  startIndex: number
  span: number
}

/** Lays out one hut's bookings within a month, clamping bookings that straddle the month boundary. */
export function blocksForHut(bookings: BookingSummary[], hutId: number, monthStart: number, monthEnd: number): TimelineBlock[] {
  return bookings
    .filter((b) => b.hutId === hutId)
    .map((booking) => {
      const arrival = Math.max(toUtcDate(booking.arrivalDate), monthStart)
      const departure = Math.min(toUtcDate(booking.departureDate), monthEnd)
      if (departure < arrival) return null
      const startIndex = Math.round((arrival - monthStart) / DAY_MS)
      const nights = Math.max(Math.round((departure - arrival) / DAY_MS), 0)
      return { booking, startIndex, span: nights + 1 }
    })
    .filter((block): block is TimelineBlock => block !== null)
}
