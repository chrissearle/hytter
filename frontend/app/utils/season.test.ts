import { describe, expect, it } from 'vitest'
import { seasonRangeFor, seasonYearFor } from './season'

describe('seasonYearFor', () => {
  it('stays on the current year before the cutover', () => {
    expect(seasonYearFor(new Date(2026, 5, 15))).toBe(2026)
  })

  it('stays on the current year on the last day of the season', () => {
    expect(seasonYearFor(new Date(2026, 7, 31))).toBe(2026)
  })

  it('rolls to next year on the cutover date itself (1 September)', () => {
    expect(seasonYearFor(new Date(2026, 8, 1))).toBe(2027)
  })

  it('rolls to next year the day before the cutover, 31 August, is not yet rolled', () => {
    expect(seasonYearFor(new Date(2026, 7, 31, 23, 59))).toBe(2026)
  })

  it('stays on next year for the rest of the year after the cutover', () => {
    expect(seasonYearFor(new Date(2026, 11, 25))).toBe(2027)
  })

  it('stays on next year in the new year before the season starts', () => {
    expect(seasonYearFor(new Date(2027, 2, 1))).toBe(2027)
  })
})

describe('seasonRangeFor', () => {
  it('builds the June-August date range for the resolved season year', () => {
    expect(seasonRangeFor(new Date(2026, 5, 15))).toEqual({
      seasonYear: 2026,
      seasonStart: '2026-06-01',
      seasonEnd: '2026-08-31'
    })
  })

  it('rolls the range forward once past the cutover', () => {
    expect(seasonRangeFor(new Date(2026, 8, 1))).toEqual({
      seasonYear: 2027,
      seasonStart: '2027-06-01',
      seasonEnd: '2027-08-31'
    })
  })
})
