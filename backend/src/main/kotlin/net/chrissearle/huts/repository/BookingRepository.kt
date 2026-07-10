package net.chrissearle.huts.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.BookingSummary
import javax.sql.DataSource

private const val FIND_IN_RANGE_SQL =
    """
    SELECT b.id, b.name, b.hut_id, h.name AS hut_name, b.arrival_date, b.departure_date, b.status
    FROM bookings b
    JOIN huts h ON h.id = b.hut_id
    WHERE b.arrival_date <= ? AND b.departure_date >= ?
    ORDER BY b.arrival_date
    """

class BookingRepository(
    private val dataSource: DataSource,
) {
    suspend fun findInRange(
        from: LocalDate,
        to: LocalDate,
    ): List<BookingSummary> =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(FIND_IN_RANGE_SQL).use { statement ->
                    statement.setObject(1, to.toJavaLocalDate())
                    statement.setObject(2, from.toJavaLocalDate())
                    statement.executeQuery().use { rows ->
                        buildList {
                            while (rows.next()) {
                                add(
                                    BookingSummary(
                                        id = rows.getInt("id"),
                                        name = rows.getString("name"),
                                        hutId = rows.getInt("hut_id"),
                                        hutName = rows.getString("hut_name"),
                                        arrivalDate =
                                            LocalDate.fromJavaLocalDate(
                                                rows.getObject("arrival_date", java.time.LocalDate::class.java),
                                            ),
                                        departureDate =
                                            LocalDate.fromJavaLocalDate(
                                                rows.getObject("departure_date", java.time.LocalDate::class.java),
                                            ),
                                        status = BookingStatus.valueOf(rows.getString("status")),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
}

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, monthNumber, day)

private fun LocalDate.Companion.fromJavaLocalDate(date: java.time.LocalDate): LocalDate =
    LocalDate(date.year, date.monthValue, date.dayOfMonth)
