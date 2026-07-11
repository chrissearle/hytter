import { describe, expect, it } from 'vitest'
import type { BookingSummary } from '~/types/booking'
import { blocksForHut, buildMonths, daysInRange, toUtcDate, todayIndexInRange } from './timeline'

function booking(overrides: Partial<BookingSummary> = {}): BookingSummary {
  return {
    id: 1,
    name: 'Opphavet',
    hutId: 1,
    hutName: 'Huldrebakken',
    arrivalDate: '2026-06-01',
    departureDate: '2026-06-05',
    status: 'OPEN',
    ...overrides
  }
}

describe('toUtcDate', () => {
  it('parses an ISO date string to a UTC timestamp', () => {
    expect(toUtcDate('2026-06-01')).toBe(Date.UTC(2026, 5, 1))
  })
})

describe('buildMonths', () => {
  it('splits a season into one entry per calendar month', () => {
    const months = buildMonths('2026-06-01', '2026-08-31')

    expect(months).toHaveLength(3)
    expect(months.map((m) => m.label)).toEqual(['juni 2026', 'juli 2026', 'august 2026'])
  })

  it('clamps the first and last month to the season boundaries', () => {
    const months = buildMonths('2026-06-15', '2026-07-10')

    expect(months[0]!.monthStart).toBe(Date.UTC(2026, 5, 15))
    expect(months[0]!.monthEnd).toBe(Date.UTC(2026, 5, 30))
    expect(months[1]!.monthStart).toBe(Date.UTC(2026, 6, 1))
    expect(months[1]!.monthEnd).toBe(Date.UTC(2026, 6, 10))
  })

  it('returns a single month when the season fits within one', () => {
    expect(buildMonths('2026-06-01', '2026-06-30')).toHaveLength(1)
  })
})

describe('daysInRange', () => {
  it('produces one Date per day inclusive of both ends', () => {
    const days = daysInRange(Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 3))

    expect(days).toHaveLength(3)
    expect(days[0]!.getUTCDate()).toBe(1)
    expect(days[2]!.getUTCDate()).toBe(3)
  })

  it('produces a single day when start equals end', () => {
    expect(daysInRange(Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 1))).toHaveLength(1)
  })
})

describe('todayIndexInRange', () => {
  it('returns the offset of today within the range', () => {
    const monthStart = Date.UTC(2026, 5, 1)
    const now = new Date(2026, 5, 4) // local time, as the browser would give us

    expect(todayIndexInRange(monthStart, 30, now)).toBe(3)
  })

  it('returns -1 when today falls outside the range', () => {
    const monthStart = Date.UTC(2026, 5, 1)
    const now = new Date(2027, 0, 1)

    expect(todayIndexInRange(monthStart, 30, now)).toBe(-1)
  })
})

describe('blocksForHut', () => {
  it('ignores bookings for other huts', () => {
    const bookings = [booking({ hutId: 2 })]

    expect(blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))).toEqual([])
  })

  it('computes the start offset and inclusive night span for a booking fully within the month', () => {
    const bookings = [booking({ arrivalDate: '2026-06-10', departureDate: '2026-06-13' })]

    const blocks = blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))

    expect(blocks).toHaveLength(1)
    expect(blocks[0]!.startIndex).toBe(9)
    expect(blocks[0]!.span).toBe(4)
  })

  it('clamps a booking that starts before the visible month to the month start', () => {
    const bookings = [booking({ arrivalDate: '2026-05-28', departureDate: '2026-06-03' })]

    const blocks = blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))

    expect(blocks[0]!.startIndex).toBe(0)
    expect(blocks[0]!.span).toBe(3)
  })

  it('clamps a booking that ends after the visible month to the month end', () => {
    const bookings = [booking({ arrivalDate: '2026-06-28', departureDate: '2026-07-03' })]

    const blocks = blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))

    expect(blocks[0]!.startIndex).toBe(27)
    expect(blocks[0]!.span).toBe(3)
  })

  it('drops a booking that does not overlap the visible month at all', () => {
    const bookings = [booking({ arrivalDate: '2026-01-01', departureDate: '2026-01-05' })]

    expect(blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))).toEqual([])
  })

  it('gives a same-day arrival/departure booking a span of one day', () => {
    const bookings = [booking({ arrivalDate: '2026-06-10', departureDate: '2026-06-10' })]

    const blocks = blocksForHut(bookings, 1, Date.UTC(2026, 5, 1), Date.UTC(2026, 5, 30))

    expect(blocks[0]!.span).toBe(1)
  })
})
