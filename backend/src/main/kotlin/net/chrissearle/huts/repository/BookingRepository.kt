package net.chrissearle.huts.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import net.chrissearle.huts.domain.BookingData
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.BookingRecord
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.BookingSummary
import net.chrissearle.huts.domain.Hut
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

private const val PARAM_NAME_TYPE = 1
private const val PARAM_NAME = 2
private const val PARAM_NUMBER_OF_PEOPLE = 3
private const val PARAM_HUT = 4
private const val PARAM_ARRIVAL_DATE = 5
private const val PARAM_DEPARTURE_DATE = 6
private const val PARAM_SEVENTH = 7
private const val PARAM_EIGHTH = 8

private fun PreparedStatement.bindData(data: BookingData) {
    setString(PARAM_NAME_TYPE, data.nameType.name)
    setString(PARAM_NAME, data.name)
    setInt(PARAM_NUMBER_OF_PEOPLE, data.numberOfPeople)
    setString(PARAM_HUT, data.hut.name)
    setObject(PARAM_ARRIVAL_DATE, data.arrivalDate.toJavaLocalDate())
    setObject(PARAM_DEPARTURE_DATE, data.departureDate.toJavaLocalDate())
}

private const val FIND_IN_RANGE_SQL =
    """
    SELECT id, name, hut, arrival_date, departure_date, status
    FROM bookings
    WHERE arrival_date <= ? AND departure_date >= ?
    ORDER BY arrival_date
    """

private const val FIND_BY_ID_SQL =
    """
    SELECT id, name_type, name, number_of_people, hut,
           arrival_date, departure_date, admin_notes, status, created_by, created_by_subject
    FROM bookings
    WHERE id = ?
    """

private const val INSERT_SQL =
    """
    INSERT INTO bookings
        (name_type, name, number_of_people, hut, arrival_date, departure_date, created_by, created_by_subject)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    RETURNING id
    """

// A user editing their booking sends it back for re-approval - that is the
// domain rule. An admin editing one is doing the approving, so their edit must
// not silently un-approve it.
private const val UPDATE_SQL =
    """
    UPDATE bookings
    SET name_type = ?, name = ?, number_of_people = ?, hut = ?, arrival_date = ?, departure_date = ?,
        status = 'OPEN', updated_at = now()
    WHERE id = ?
    """

private const val UPDATE_KEEPING_STATUS_SQL =
    """
    UPDATE bookings
    SET name_type = ?, name = ?, number_of_people = ?, hut = ?, arrival_date = ?, departure_date = ?,
        updated_at = now()
    WHERE id = ?
    """

private const val APPROVE_SQL = "UPDATE bookings SET status = 'APPROVED', updated_at = now() WHERE id = ?"

// Deliberately leaves status alone: annotating a booking and approving it are
// independent admin actions, and a note must not silently approve anything.
private const val UPDATE_ADMIN_NOTES_SQL = "UPDATE bookings SET admin_notes = ?, updated_at = now() WHERE id = ?"

private const val DELETE_SQL = "DELETE FROM bookings WHERE id = ?"

class BookingRepository(
    private val dataSource: DataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun findInRange(
        from: LocalDate,
        to: LocalDate,
    ): List<BookingSummary> =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(FIND_IN_RANGE_SQL).use { statement ->
                    statement.setObject(1, to.toJavaLocalDate())
                    statement.setObject(2, from.toJavaLocalDate())
                    statement.executeQuery().use { rows ->
                        buildList {
                            while (rows.next()) {
                                add(rows.toBookingSummary())
                            }
                        }
                    }
                }
            }
        }

    suspend fun findById(id: Int): BookingRecord? =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) rows.toBookingRecord() else null
                    }
                }
            }
        }

    suspend fun insert(
        data: BookingData,
        createdBy: String?,
        createdBySubject: String?,
    ): Int =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(INSERT_SQL).use { statement ->
                    statement.bindData(data)
                    statement.setString(PARAM_SEVENTH, createdBy)
                    statement.setString(PARAM_EIGHTH, createdBySubject)
                    statement.executeQuery().use { rows ->
                        rows.next()
                        rows.getInt("id")
                    }
                }
            }
        }

    /** [keepStatus] is for admin edits - see the SQL above. */
    suspend fun update(
        id: Int,
        data: BookingData,
        keepStatus: Boolean = false,
    ): Int =
        withContext(dispatcher) {
            val sql = if (keepStatus) UPDATE_KEEPING_STATUS_SQL else UPDATE_SQL
            dataSource.connection.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.bindData(data)
                    statement.setInt(PARAM_SEVENTH, id)
                    statement.executeUpdate()
                }
            }
        }

    suspend fun approve(id: Int): Int =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(APPROVE_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeUpdate()
                }
            }
        }

    suspend fun updateAdminNotes(
        id: Int,
        adminNotes: String?,
    ): Int =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(UPDATE_ADMIN_NOTES_SQL).use { statement ->
                    statement.setString(1, adminNotes)
                    statement.setInt(2, id)
                    statement.executeUpdate()
                }
            }
        }

    suspend fun delete(id: Int): Int =
        withContext(dispatcher) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(DELETE_SQL).use { statement ->
                    statement.setInt(1, id)
                    statement.executeUpdate()
                }
            }
        }
}

private fun ResultSet.toBookingSummary() =
    BookingSummary(
        id = getInt("id"),
        name = getString("name"),
        hut = Hut.valueOf(getString("hut")),
        arrivalDate = localDate("arrival_date"),
        departureDate = localDate("departure_date"),
        status = BookingStatus.valueOf(getString("status")),
    )

private fun ResultSet.toBookingRecord() =
    BookingRecord(
        id = getInt("id"),
        nameType = BookingNameType.valueOf(getString("name_type")),
        name = getString("name"),
        numberOfPeople = getInt("number_of_people"),
        hut = Hut.valueOf(getString("hut")),
        arrivalDate = localDate("arrival_date"),
        departureDate = localDate("departure_date"),
        adminNotes = getString("admin_notes"),
        status = BookingStatus.valueOf(getString("status")),
        createdBy = getString("created_by"),
        createdBySubject = getString("created_by_subject"),
    )

private fun ResultSet.localDate(column: String): LocalDate {
    val date = getObject(column, java.time.LocalDate::class.java)
    return LocalDate(date.year, date.monthValue, date.dayOfMonth)
}

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, month.number, day)
