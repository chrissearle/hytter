package net.chrissearle.huts.routes

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.BookingNotFound
import net.chrissearle.huts.api.DatabaseCallFailed
import net.chrissearle.huts.api.InvalidBookingId
import net.chrissearle.huts.domain.BookingRecord
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.HytterPrincipal
import java.sql.SQLException

/**
 * Ownership is keyed on the Keycloak `sub`. Bookings written before the subject
 * column existed have none, so those fall back to comparing the display name -
 * the old, weaker check, applied only where there is nothing better.
 */
fun BookingRecord.isOwnedBy(principal: HytterPrincipal): Boolean =
    when (val subject = createdBySubject) {
        null -> createdBy != null && createdBy == principal.name
        else -> subject == principal.subject
    }

fun BookingRecord.canBeEditedBy(principal: HytterPrincipal?): Boolean {
    if (principal == null) return false
    return principal.isAdmin || isOwnedBy(principal)
}

/** A path id that is not a number is a bad request, not a missing booking. */
context(_: Raise<ApiError>)
fun bookingId(raw: String?): Int = raw?.toIntOrNull() ?: raise(InvalidBookingId(raw))

context(_: Raise<ApiError>)
suspend fun BookingRepository.findBooking(id: Int): BookingRecord {
    val record =
        catch({ findById(id) }) { e: SQLException ->
            raise(DatabaseCallFailed(e.asErrorResponse()))
        }
    return record ?: raise(BookingNotFound(id))
}
