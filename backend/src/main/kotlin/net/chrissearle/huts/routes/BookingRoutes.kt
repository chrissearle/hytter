package net.chrissearle.huts.routes

import arrow.core.raise.catch
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.ErrorResponse
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.AUTH_PROVIDER_NAME
import java.sql.SQLException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val SEASON_START_MONTH = 6
private const val SEASON_END_MONTH = 8
private const val SEASON_END_DAY = 31

fun Route.bookingRoutes(repository: BookingRepository) {
    authenticate(AUTH_PROVIDER_NAME, optional = true) {
        get("/api/bookings") {
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]

            either {
                val fromDate = from?.let { parseDate(it) } ?: defaultSeasonStart()
                val toDate = to?.let { parseDate(it) } ?: defaultSeasonEnd()
                if (toDate < fromDate) {
                    raise(InvalidDateRange("'to' must not be before 'from'"))
                }
                catch({ repository.findInRange(fromDate, toDate) }) { e: SQLException ->
                    val error =
                        ErrorResponse(
                            status = HttpStatusCode.InternalServerError,
                            message = e.message ?: "database error",
                        )
                    raise(DatabaseCallFailed(error))
                }
            }.respond()
        }

        get("/api/bookings/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            either {
                if (id == null) {
                    raise(BookingNotFound(-1))
                }
                catch({ repository.findById(id) }) { e: SQLException ->
                    val error =
                        ErrorResponse(
                            status = HttpStatusCode.InternalServerError,
                            message = e.message ?: "database error",
                        )
                    raise(DatabaseCallFailed(error))
                } ?: raise(BookingNotFound(id))
            }.respond()
        }
    }
}

private fun parseDate(value: String): LocalDate = LocalDate.parse(value)

@OptIn(ExperimentalTime::class)
private fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

private fun defaultSeasonStart(): LocalDate = LocalDate(currentYear(), SEASON_START_MONTH, 1)

private fun defaultSeasonEnd(): LocalDate = LocalDate(currentYear(), SEASON_END_MONTH, SEASON_END_DAY)
