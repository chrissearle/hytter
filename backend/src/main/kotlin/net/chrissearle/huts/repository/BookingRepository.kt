package net.chrissearle.huts.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.domain.Booking
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.BookingSummary
import java.sql.PreparedStatement
import javax.sql.DataSource

private const val PARAM_NAME = 1
private const val PARAM_NUMBER_OF_PEOPLE = 2
private const val PARAM_HUT_ID = 3
private const val PARAM_ARRIVAL_DATE = 4
private const val PARAM_DEPARTURE_DATE = 5
private const val PARAM_SIXTH = 6

private fun PreparedStatement.bindInput(input: BookingInput) {
    setString(PARAM_NAME, input.name)
    setInt(PARAM_NUMBER_OF_PEOPLE, input.numberOfPeople)
    setInt(PARAM_HUT_ID, input.hutId)
    setObject(PARAM_ARRIVAL_DATE, input.arrivalDate.toJavaLocalDate())
    setObject(PARAM_DEPARTURE_DATE, input.departureDate.toJavaLocalDate())
}

private const val FIND_IN_RANGE_SQL =
    """
    SELECT b.id, b.name, b.hut_id, h.name AS hut_name, b.arrival_date, b.departure_date, b.status
    FROM bookings b
    JOIN huts h ON h.id = b.hut_id
    WHERE b.arrival_date <= ? AND b.departure_date >= ?
    ORDER BY b.arrival_date
    """

private const val FIND_BY_ID_SQL =
    """
    SELECT b.id, b.name, b.number_of_people, b.hut_id, h.name AS hut_name,
           b.arrival_date, b.departure_date, b.admin_notes, b.status, b.created_by
    FROM bookings b
    JOIN huts h ON h.id = b.hut_id
    WHERE b.id = ?
    """

private const val INSERT_SQL =
    """
    INSERT INTO bookings (name, number_of_people, hut_id, arrival_date, departure_date, created_by)
    VALUES (?, ?, ?, ?, ?, ?)
    RETURNING id
    """

private const val UPDATE_SQL =
    """
    UPDATE bookings
    SET name = ?, number_of_people = ?, hut_id = ?, arrival_date = ?, departure_date = ?,
        status = 'OPEN', updated_at = now()
    WHERE id = ?
    """

private const val APPROVE_SQL = "UPDATE bookings SET status = 'APPROVED', updated_at = now() WHERE id = ?"

private const val DELETE_SQL = "DELETE FROM bookings WHERE id = ?"

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

    suspend fun findById(id: Int): Booking? =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeQuery().use { rows ->
                        if (!rows.next()) {
                            null
                        } else {
                            Booking(
                                id = rows.getInt("id"),
                                name = rows.getString("name"),
                                numberOfPeople = rows.getInt("number_of_people"),
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
                                adminNotes = rows.getString("admin_notes"),
                                status = BookingStatus.valueOf(rows.getString("status")),
                                createdBy = rows.getString("created_by"),
                            )
                        }
                    }
                }
            }
        }

    suspend fun insert(
        input: BookingInput,
        createdBy: String?,
    ): Int =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(INSERT_SQL).use { statement ->
                    statement.bindInput(input)
                    statement.setString(PARAM_SIXTH, createdBy)
                    statement.executeQuery().use { rows ->
                        rows.next()
                        rows.getInt("id")
                    }
                }
            }
        }

    suspend fun update(
        id: Int,
        input: BookingInput,
    ): Int =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(UPDATE_SQL).use { statement ->
                    statement.bindInput(input)
                    statement.setInt(PARAM_SIXTH, id)
                    statement.executeUpdate()
                }
            }
        }

    suspend fun approve(id: Int): Int =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(APPROVE_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeUpdate()
                }
            }
        }

    suspend fun delete(id: Int): Int =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(DELETE_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeUpdate()
                }
            }
        }
}

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, monthNumber, day)

private fun LocalDate.Companion.fromJavaLocalDate(date: java.time.LocalDate): LocalDate =
    LocalDate(date.year, date.monthValue, date.dayOfMonth)
