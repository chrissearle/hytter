package net.chrissearle.huts.routes

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.ErrorResponse
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.InvalidNumberOfPeople
import net.chrissearle.huts.api.NameRequired
import net.chrissearle.huts.domain.BookingData
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingNameType
import java.sql.SQLException

const val NAME_MAX_LENGTH = 100

private val logger = KotlinLogging.logger {}

/**
 * Logs the real failure and hands back a response the client never sees the
 * detail of - see [net.chrissearle.huts.api.messageMap]. The call-id in the MDC
 * ties the log line back to the request.
 */
fun SQLException.asErrorResponse(): ErrorResponse {
    logger.error(this) { "Database call failed" }
    return ErrorResponse(status = HttpStatusCode.InternalServerError, message = message ?: "database error")
}

/**
 * Validates the input and works out the name the booking is stored under.
 * The hut and the name type are enums, so an unknown value fails during
 * deserialization and never reaches this point.
 */
context(_: Raise<ApiError>)
fun BookingInput.resolve(principalName: String?): BookingData {
    if (numberOfPeople <= 0) {
        raise(InvalidNumberOfPeople)
    }
    if (departureDate < arrivalDate) {
        raise(InvalidDateRange("'departureDate' must not be before 'arrivalDate'"))
    }

    return BookingData(
        nameType = nameType,
        name = resolveName(principalName),
        numberOfPeople = numberOfPeople,
        hut = hut,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
    )
}

/**
 * A fixed group always stores its own label - a client cannot claim to be one.
 * "Personlig" uses the logged-in user's `name` claim; an anonymous visitor has
 * no claim to draw on, so they type it in, as they do for "Annet".
 */
context(_: Raise<ApiError>)
private fun BookingInput.resolveName(principalName: String?): String {
    val fromPrincipal =
        principalName
            ?.trim()
            ?.takeIf { nameType == BookingNameType.PERSONAL && it.isNotEmpty() }

    return when {
        !nameType.isFreeText -> {
            nameType.displayName
        }

        fromPrincipal != null -> {
            fromPrincipal
        }

        else -> {
            val supplied = name?.trim().orEmpty()
            if (supplied.isEmpty() || supplied.length > NAME_MAX_LENGTH) {
                raise(NameRequired)
            }
            supplied
        }
    }
}
