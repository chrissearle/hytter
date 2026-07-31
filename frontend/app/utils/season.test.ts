import { describe, expect, it } from 'vitest'
import {
  MIN_SEASON_INDEX,
  currentSeason,
  formatSeasonRange,
  maxSeasonIndex,
  resolveSeason,
  seasonFromIndex,
  seasonFromSlug
} from './season'

describe('currentSeason', () => {
  it('maps a June date to summer', () => {
    expect(currentSeason(new Date(2026, 5, 15))).toMatchObject({
      key: 'summer',
      year: 2026,
      label: 'Sommer 2026',
      start: '2026-06-01',
      end: '2026-08-31'
    })
  })

  it('maps a September date to autumn', () => {
    expect(currentSeason(new Date(2026, 8, 1))).toMatchObject({
      key: 'autumn',
      label: 'Høst 2026',
      start: '2026-09-01',
      end: '2026-11-30'
    })
  })

  it('maps an April date to spring', () => {
    expect(currentSeason(new Date(2027, 3, 10))).toMatchObject({
      key: 'spring',
      label: 'Vår 2027',
      start: '2027-03-01',
      end: '2027-05-31'
    })
  })

  it('rolls December into the following year’s winter', () => {
    expect(currentSeason(new Date(2026, 11, 1))).toMatchObject({
      key: 'winter',
      year: 2027,
      label: 'Vinter 2027',
      start: '2026-12-01',
      end: '2027-02-28'
    })
  })

  it('keeps January in the winter that started the previous December', () => {
    expect(currentSeason(new Date(2027, 0, 15))).toMatchObject({
      key: 'winter',
      year: 2027,
      start: '2026-12-01'
    })
  })

  it('keeps the last day of February in winter', () => {
    expect(currentSeason(new Date(2027, 1, 28))).toMatchObject({ key: 'winter', year: 2027 })
  })

  it('moves to spring on 1 March', () => {
    expect(currentSeason(new Date(2027, 2, 1))).toMatchObject({ key: 'spring', year: 2027 })
  })
})

describe('season ranges', () => {
  it('ends winter on 29 February in a leap year', () => {
    const winter2028 = seasonFromSlug('2028-winter')
    expect(winter2028?.start).toBe('2027-12-01')
    expect(winter2028?.end).toBe('2028-02-29')
  })

  it('ends winter on 28 February in a non-leap year', () => {
    expect(seasonFromSlug('2027-winter')?.end).toBe('2027-02-28')
  })
})

describe('season ordering', () => {
  it('advances summer -> autumn -> winter -> spring across the year boundary', () => {
    const summer = currentSeason(new Date(2026, 6, 1))
    const autumn = seasonFromIndex(summer.index + 1)
    const winter = seasonFromIndex(summer.index + 2)
    const spring = seasonFromIndex(summer.index + 3)

    expect([autumn.label, winter.label, spring.label]).toEqual([
      'Høst 2026',
      'Vinter 2027',
      'Vår 2027'
    ])
  })

  it('steps backwards out of winter into the previous autumn', () => {
    const winter = seasonFromSlug('2027-winter')
    expect(seasonFromIndex(winter!.index - 1).label).toBe('Høst 2026')
  })

  it('is chronologically ordered by index', () => {
    const slugs = ['2026-summer', '2026-autumn', '2027-winter', '2027-spring', '2027-summer']
    const indexes = slugs.map((slug) => seasonFromSlug(slug)!.index)
    expect(indexes).toEqual([...indexes].sort((a, b) => a - b))
    expect(new Set(indexes).size).toBe(slugs.length)
  })
})

describe('seasonFromSlug', () => {
  it('round-trips a slug', () => {
    const season = seasonFromSlug('2027-autumn')
    expect(season?.slug).toBe('2027-autumn')
    expect(season?.label).toBe('Høst 2027')
  })

  it('returns null for a malformed slug', () => {
    expect(seasonFromSlug('garbage')).toBeNull()
    expect(seasonFromSlug('2026-sommer')).toBeNull()
    expect(seasonFromSlug('26-summer')).toBeNull()
  })

  it('returns null for non-string input', () => {
    expect(seasonFromSlug(undefined)).toBeNull()
    expect(seasonFromSlug(null)).toBeNull()
    expect(seasonFromSlug(['2026-summer'])).toBeNull()
  })
})

describe('resolveSeason', () => {
  const now = new Date(2026, 6, 1)

  it('uses a valid in-range slug', () => {
    expect(resolveSeason('2026-autumn', now).label).toBe('Høst 2026')
  })

  it('falls back to the current season when the slug is missing or junk', () => {
    expect(resolveSeason(undefined, now).label).toBe('Sommer 2026')
    expect(resolveSeason('garbage', now).label).toBe('Sommer 2026')
  })

  it('falls back when the slug is before the earliest season', () => {
    expect(resolveSeason('2026-spring', now).label).toBe('Sommer 2026')
  })

  it('falls back when the slug is beyond the browsable window', () => {
    const beyond = seasonFromIndex(maxSeasonIndex(now) + 1)
    expect(resolveSeason(beyond.slug, now).label).toBe('Sommer 2026')
  })

  it('accepts the exact bounds', () => {
    expect(resolveSeason(seasonFromIndex(MIN_SEASON_INDEX).slug, now).label).toBe('Sommer 2026')
    expect(resolveSeason(seasonFromIndex(maxSeasonIndex(now)).slug, now).label).toBe('Sommer 2028')
  })
})

describe('bounds', () => {
  it('starts at Sommer 2026', () => {
    expect(seasonFromIndex(MIN_SEASON_INDEX).slug).toBe('2026-summer')
  })

  it('allows two years ahead of the current season', () => {
    const now = new Date(2026, 6, 1)
    expect(seasonFromIndex(maxSeasonIndex(now)).label).toBe('Sommer 2028')
  })
})

describe('formatSeasonRange', () => {
  it('omits the year when the season sits inside one', () => {
    expect(formatSeasonRange(seasonFromSlug('2026-summer')!)).toBe('1. juni – 31. august')
  })

  it('includes the year when the season spans two', () => {
    expect(formatSeasonRange(seasonFromSlug('2027-winter')!)).toBe(
      '1. desember 2026 – 28. februar 2027'
    )
  })
})
