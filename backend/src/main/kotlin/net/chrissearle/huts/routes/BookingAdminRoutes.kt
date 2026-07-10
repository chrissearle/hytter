package net.chrissearle.huts.routes

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import net.chrissearle.huts.api.AdminRequired
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.PrincipalMissing
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.HytterPrincipal
import java.sql.SQLException

fun Route.bookingAdminRoutes(repository: BookingRepository) {
    approveBookingRoute(repository)
    deleteBookingRoute(repository)
}

private fun Route.approveBookingRoute(repository: BookingRepository) {
    post("/api/bookings/{id}/approve") {
        val id = call.parameters["id"]?.toIntOrNull()
        val principal = call.principal<HytterPrincipal>()

        either {
            requireAdmin(principal)
            if (id == null) {
                raise(BookingNotFound(-1))
            }
            catch({ repository.approve(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            }
            catch({ repository.findById(id) }) { e: SQLException ->
                raise(DatabaseCallFailed(e.asErrorResponse()))
            } ?: raise(BookingNotFound(id))
        }.respond()
    }
}

private fun Route.deleteBookingRoute(repository: BookingRepository) {
    delete("/api/bookings/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        val principal = call.principal<HytterPrincipal>()

        either {
            requireAdmin(principal)
            if (id == null) {
                raise(BookingNotFound(-1))
            }
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
