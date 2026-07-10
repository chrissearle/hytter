package net.chrissearle.huts.routes

import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.NotBookingOwner
import net.chrissearle.huts.api.PrincipalMissing
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.AUTH_PROVIDER_NAME
import net.chrissearle.huts.security.HytterPrincipal
import java.sql.SQLException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val SEASON_START_MONTH = 6
private const val SEASON_END_MONTH = 8
private const val SEASON_END_DAY = 31

fun Route.bookingRoutes(repository: BookingRepository) {
    authenticate(AUTH_PROVIDER_NAME, optional = true) {
        listBookingsRoute(repository)
        getBookingRoute(repository)
        sessionRoute()
        createBookingRoute(repository)
        updateBookingRoute(repository)
    }
}

private fun Route.listBookingsRoute(repository: BookingRepository) {
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
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
        }.respond()
    }
}

private fun Route.getBookingRoute(repository: BookingRepository) {
    get("/api/bookings/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()

        either {
            if (id == null) {
                raise(BookingNotFound(-1))
            }
            catch({ repository.findById(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            } ?: raise(BookingNotFound(id))
        }.respond()
    }
}

private fun Route.createBookingRoute(repository: BookingRepository) {
    post("/api/bookings") {
        val principal = call.principal<HytterPrincipal>()

        either {
            val input = call.receive<BookingInput>()
            validateBookingInput(input)
            val id =
                catch({ repository.insert(input, createdBy = principal?.name) }) { e: SQLException ->
                    raise(DatabaseCallFailed(e.asErrorResponse()))
                }
            catch({ repository.findById(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            } ?: raise(BookingNotFound(id))
        }.respond(HttpStatusCode.Created)
    }
}

private fun Route.updateBookingRoute(repository: BookingRepository) {
    put("/api/bookings/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val principal = call.principal<HytterPrincipal>()

        either {
            if (id == null) {
                raise(BookingNotFound(-1))
            }
            if (principal == null) {
                raise(PrincipalMissing)
            }
            val existing =
                catch({ repository.findById(id) }) { e: SQLException ->
                    raise(DatabaseCallFailed(e.asErrorResponse()))
                } ?: raise(BookingNotFound(id))
            if (!principal.isAdmin && existing.createdBy != principal.name) {
                raise(NotBookingOwner)
            }
            val input = call.receive<BookingInput>()
            validateBookingInput(input)
            catch({ repository.update(id, input) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
            catch({ repository.findById(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            } ?: raise(BookingNotFound(id))
        }.respond()
    }
}

private fun parseDate(value: String): LocalDate = LocalDate.parse(value)

@OptIn(ExperimentalTime::class)
private fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

private fun defaultSeasonStart(): LocalDate = LocalDate(currentYear(), SEASON_START_MONTH, 1)

private fun defaultSeasonEnd(): LocalDate = LocalDate(currentYear(), SEASON_END_MONTH, SEASON_END_DAY)
