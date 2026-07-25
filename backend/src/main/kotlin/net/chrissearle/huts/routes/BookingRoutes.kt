package net.chrissearle.huts.routes

import arrow.core.raise.Raise
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
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.InvalidBookingId
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.NotBookingOwner
import net.chrissearle.huts.api.PrincipalMissing
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingRecord
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
    referenceRoutes()

    authenticate(AUTH_PROVIDER_NAME, optional = true) {
        listBookingsRoute(repository)
        getBookingRoute(repository)
        sessionRoute()
        createBookingRoute(repository)
        updateBookingRoute(repository)
        bookingAdminRoutes(repository)
    }
}

/**
 * Public: anonymous visitors see the calendar blocks - which hut, which group,
 * which dates, approved or not. The detail behind a block is not public.
 */
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

/** Requires a login: headcount, admin notes and the requester are not public. */
private fun Route.getBookingRoute(repository: BookingRepository) {
    get("/api/bookings/{id}") {
        val rawId = call.parameters["id"]
        val principal = call.principal<HytterPrincipal>()

        either {
            val user = requireAccess(principal)
            val record = repository.findBooking(bookingId(rawId))
            record.toBooking(canEdit = record.canBeEditedBy(user))
        }.respond()
    }
}

private fun Route.createBookingRoute(repository: BookingRepository) {
    post("/api/bookings") {
        val principal = call.principal<HytterPrincipal>()

        either {
            val data = call.receive<BookingInput>().resolve(principal)
            val id =
                catch({
                    repository.insert(
                        data = data,
                        createdBy = principal?.name,
                        createdBySubject = principal?.subject,
                    )
                }) { e: SQLException ->
                    raise(DatabaseCallFailed(e.asErrorResponse()))
                }
            repository.findBooking(id).toBooking(canEdit = principal != null)
        }.respond(HttpStatusCode.Created)
    }
}

private fun Route.updateBookingRoute(repository: BookingRepository) {
    put("/api/bookings/{id}") {
        val rawId = call.parameters["id"]
        val principal = call.principal<HytterPrincipal>()

        either {
            val user = requireAccess(principal)
            val id = bookingId(rawId)
            val existing = repository.findBooking(id)
            if (!existing.canBeEditedBy(user)) {
                raise(NotBookingOwner)
            }
            val data = call.receive<BookingInput>().resolve(user)
            catch({ repository.update(id, data, keepStatus = user.isAdmin) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
            repository.findBooking(id).toBooking(canEdit = true)
        }.respond()
    }
}

context(_: Raise<ApiError>)
private fun parseDate(value: String): LocalDate =
    catch({ LocalDate.parse(value) }) { _: IllegalArgumentException ->
        raise(InvalidDateRange("'$value' is not a valid date"))
    }

@OptIn(ExperimentalTime::class)
private fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

private fun defaultSeasonStart(): LocalDate = LocalDate(currentYear(), SEASON_START_MONTH, 1)

private fun defaultSeasonEnd(): LocalDate = LocalDate(currentYear(), SEASON_END_MONTH, SEASON_END_DAY)
