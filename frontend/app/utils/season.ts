// A season is a calendar quarter: Vinter (Dec-Feb), Vår (Mar-May),
// Sommer (Jun-Aug), Høst (Sep-Nov). Winter straddles a year boundary and is
// named for the year it ENDS in, matching normal Norwegian usage - "vinteren
// 2027" is Jan/Feb 2027, so Vinter 2027 runs 1 Dec 2026 - 28 Feb 2027.

// Imported explicitly rather than relying on Nuxt auto-imports: the vitest
// suite runs these utils as plain modules in a node environment.
import { toUtcDate } from './timeline'

export type SeasonKey = 'winter' | 'spring' | 'summer' | 'autumn'

// Order matters: the array index is the season's position within its label
// year, which is what makes `index` arithmetic below chronological.
const SEASON_ORDER: SeasonKey[] = ['winter', 'spring', 'summer', 'autumn']

const SEASON_LABELS: Record<SeasonKey, string> = {
  winter: 'Vinter',
  spring: 'Vår',
  summer: 'Sommer',
  autumn: 'Høst'
}

export interface Season {
  key: SeasonKey
  /** For winter, the year the season ends in. */
  year: number
  /** Chronological ordinal - prev/next is ±1, comparisons are plain integers. */
  index: number
  label: string
  start: string
  end: string
  slug: string
}

/** The site does not go back further than the first season it was built for. */
export const MIN_SEASON_INDEX = 2026 * SEASON_ORDER.length + SEASON_ORDER.indexOf('summer')

/** How far ahead bookings may be browsed: two years past the current season. */
const SEASONS_AHEAD = SEASON_ORDER.length * 2

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function iso(year: number, month: number, day: number): string {
  return `${year}-${pad(month)}-${pad(day)}`
}

/** Day 0 of the next month is the last day of this one, so February stays leap-safe. */
function lastDayOfMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

function rangeFor(key: SeasonKey, year: number): { start: string; end: string } {
  switch (key) {
    case 'winter':
      return { start: iso(year - 1, 12, 1), end: iso(year, 2, lastDayOfMonth(year, 2)) }
    case 'spring':
      return { start: iso(year, 3, 1), end: iso(year, 5, 31) }
    case 'summer':
      return { start: iso(year, 6, 1), end: iso(year, 8, 31) }
    case 'autumn':
      return { start: iso(year, 9, 1), end: iso(year, 11, 30) }
  }
}

export function seasonFromIndex(index: number): Season {
  const year = Math.floor(index / SEASON_ORDER.length)
  const key = SEASON_ORDER[index % SEASON_ORDER.length] as SeasonKey
  const { start, end } = rangeFor(key, year)

  return {
    key,
    year,
    index,
    label: `${SEASON_LABELS[key]} ${year}`,
    start,
    end,
    slug: `${year}-${key}`
  }
}

export function currentSeason(now: Date): Season {
  const month = now.getMonth() + 1
  const year = now.getFullYear()

  // December already belongs to next year's winter; January and February to
  // this year's.
  if (month === 12) return seasonFromIndex((year + 1) * SEASON_ORDER.length)
  if (month <= 2) return seasonFromIndex(year * SEASON_ORDER.length)

  const key: SeasonKey = month <= 5 ? 'spring' : month <= 8 ? 'summer' : 'autumn'
  return seasonFromIndex(year * SEASON_ORDER.length + SEASON_ORDER.indexOf(key))
}

export function maxSeasonIndex(now: Date): number {
  return currentSeason(now).index + SEASONS_AHEAD
}

const SLUG_PATTERN = /^(\d{4})-(winter|spring|summer|autumn)$/

/**
 * Parses a `?sesong=` value. The slug uses the English key because `vår` and
 * `høst` are not URL-safe; only the label is Bokmål.
 */
export function seasonFromSlug(slug: unknown): Season | null {
  if (typeof slug !== 'string') return null

  const match = SLUG_PATTERN.exec(slug)
  if (!match) return null

  const year = Number(match[1])
  const key = match[2] as SeasonKey
  return seasonFromIndex(year * SEASON_ORDER.length + SEASON_ORDER.indexOf(key))
}

/**
 * Resolves the season to display. Anything missing, malformed or outside the
 * browsable window falls back to the current season rather than erroring - the
 * query param is user-editable and a bad value should not break the calendar.
 */
export function resolveSeason(slug: unknown, now: Date): Season {
  const parsed = seasonFromSlug(slug)
  if (parsed && parsed.index >= MIN_SEASON_INDEX && parsed.index <= maxSeasonIndex(now)) {
    return parsed
  }
  return currentSeason(now)
}

/**
 * Bokmål range for the header, e.g. "1. juni - 31. august". Winter spans two
 * years, so the year is included when the endpoints disagree on it.
 */
export function formatSeasonRange(season: Season): string {
  const spansYears = season.start.slice(0, 4) !== season.end.slice(0, 4)
  const options: Intl.DateTimeFormatOptions = {
    day: 'numeric',
    month: 'long',
    timeZone: 'UTC',
    ...(spansYears ? { year: 'numeric' } : {})
  }

  const format = (value: string) => new Date(toUtcDate(value)).toLocaleDateString('nb-NO', options)

  return `${format(season.start)} – ${format(season.end)}`
}
