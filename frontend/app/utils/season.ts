// Season is always June-August. From 1 September the site switches to
// showing next year's season, so the calendar is never showing a season
// that just ended.
const SEASON_CUTOVER_MONTH = 8 // September, 0-indexed
const SEASON_START_MONTH = '06'
const SEASON_END_MONTH = '08'
const SEASON_END_DAY = '31'

export function seasonYearFor(now: Date): number {
  const year = now.getFullYear()
  return now >= new Date(year, SEASON_CUTOVER_MONTH, 1) ? year + 1 : year
}

export function seasonRangeFor(now: Date): {
  seasonYear: number
  seasonStart: string
  seasonEnd: string
} {
  const seasonYear = seasonYearFor(now)
  return {
    seasonYear,
    seasonStart: `${seasonYear}-${SEASON_START_MONTH}-01`,
    seasonEnd: `${seasonYear}-${SEASON_END_MONTH}-${SEASON_END_DAY}`
  }
}
