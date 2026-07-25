package net.chrissearle.huts.routes

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import net.chrissearle.huts.api.AdminNotesTooLong
import net.chrissearle.huts.api.AdminRequired
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.PrincipalMissing
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.domain.AdminNotesInput
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.HytterPrincipal
import java.sql.SQLException

const val ADMIN_NOTES_MAX_LENGTH = 2000

fun Route.bookingAdminRoutes(repository: BookingRepository) {
    approveBookingRoute(repository)
    adminNotesRoute(repository)
    deleteBookingRoute(repository)
}

/**
 * Admin notes are how the admin tells a requester about things the system does
 * not manage itself - a handover overlap, say. Visible to the requester, so it
 * lives on the booking rather than in a private admin view.
 */
private fun Route.adminNotesRoute(repository: BookingRepository) {
    patch("/api/bookings/{id}/notes") {
        val rawId = call.parameters["id"]
        val principal = call.principal<HytterPrincipal>()

        either {
            requireAdmin(principal)
            val id = bookingId(rawId)
            val notes = call.receive<AdminNotesInput>().validated()
            catch({ repository.updateAdminNotes(id, notes) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
            repository.findBooking(id).toBooking(canEdit = true)
        }.respond()
    }
}

/** Blank clears the note rather than storing whitespace. */
context(_: Raise<ApiError>)
private fun AdminNotesInput.validated(): String? {
    val trimmed = adminNotes?.trim()
    if (trimmed != null && trimmed.length > ADMIN_NOTES_MAX_LENGTH) {
        raise(AdminNotesTooLong(ADMIN_NOTES_MAX_LENGTH))
    }
    return trimmed?.takeIf { it.isNotEmpty() }
}

private fun Route.approveBookingRoute(repository: BookingRepository) {
    post("/api/bookings/{id}/approve") {
        val rawId = call.parameters["id"]
        val principal = call.principal<HytterPrincipal>()

        either {
            requireAdmin(principal)
            val id = bookingId(rawId)
            catch({ repository.approve(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
            repository.findBooking(id).toBooking(canEdit = true)
        }.respond()
    }
}

private fun Route.deleteBookingRoute(repository: BookingRepository) {
    delete("/api/bookings/{id}") {
        val rawId = call.parameters["id"]
        val principal = call.principal<HytterPrincipal>()

        either {
            requireAdmin(principal)
            val id = bookingId(rawId)
            val deletedRows =
                catch({ repository.delete(id) }) { e: SQLException ->
                    raise(DatabaseCallFailed(e.asErrorResponse()))
                }
            if (deletedRows == 0) {
                raise(BookingNotFound(id))
            }
        }.respond()
    }
}

context(_: Raise<ApiError>)
private fun requireAdmin(principal: HytterPrincipal?) {
    if (principal == null) {
        raise(PrincipalMissing)
    }
    if (!principal.isAdmin) {
        raise(AdminRequired)
    }
}
