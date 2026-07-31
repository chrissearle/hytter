package net.chrissearle.huts.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MONTHS_IN_SEASON = 3
private const val MONTHS_IN_YEAR = 12
private const val DECEMBER = 12

/**
 * A season is a calendar quarter - winter Dec-Feb, spring Mar-May, summer
 * Jun-Aug, autumn Sep-Nov. Mirrors the frontend's `app/utils/season.ts`; the
 * two must agree, since the frontend sends explicit `from`/`to` and this is
 * only the fallback when it does not.
 */
fun seasonStart(date: LocalDate): LocalDate {
    val month = date.monthNumber

    // Modulo 12 shifts December to 0, lining the quarters up so that integer
    // division picks the right one.
    val quarter = (month % MONTHS_IN_YEAR) / MONTHS_IN_SEASON

    // Winter starts in the December before the January and February it covers.
    return if (quarter == 0) {
        LocalDate(if (month == DECEMBER) date.year else date.year - 1, DECEMBER, 1)
    } else {
        LocalDate(date.year, quarter * MONTHS_IN_SEASON, 1)
    }
}

/** Adding a quarter and stepping back a day keeps February leap-safe. */
fun seasonEnd(start: LocalDate): LocalDate {
    val nextSeasonStart = start.plus(MONTHS_IN_SEASON, DateTimeUnit.MONTH)
    return nextSeasonStart.minus(1, DateTimeUnit.DAY)
}

/** The season containing today, as a `from`/`to` pair. */
@OptIn(ExperimentalTime::class)
fun currentSeasonRange(): Pair<LocalDate, LocalDate> {
    val start = seasonStart(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    return start to seasonEnd(start)
}
