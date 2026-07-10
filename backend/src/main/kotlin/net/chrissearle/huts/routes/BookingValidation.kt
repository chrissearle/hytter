package net.chrissearle.huts.routes

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import io.ktor.http.HttpStatusCode
import net.chrissearle.huts.api.ApiError
import net.chrissearle.huts.api.ErrorResponse
import net.chrissearle.huts.api.HutRequired
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.InvalidNumberOfPeople
import net.chrissearle.huts.api.NameRequired
import net.chrissearle.huts.domain.BookingInput
import java.sql.SQLException

private const val NAME_MAX_LENGTH = 100

fun SQLException.asErrorResponse(): ErrorResponse {
    val databaseErrorMessage = "database error"
    return ErrorResponse(status = HttpStatusCode.InternalServerError, message = message ?: databaseErrorMessage)
}

context(_: Raise<ApiError>)
fun validateBookingInput(input: BookingInput) {
    if (input.name.isBlank() || input.name.length > NAME_MAX_LENGTH) {
        raise(NameRequired)
    }
    if (input.numberOfPeople <= 0) {
        raise(InvalidNumberOfPeople)
    }
    if (input.hutId <= 0) {
        raise(HutRequired)
    }
    if (input.departureDate < input.arrivalDate) {
        raise(InvalidDateRange("'departureDate' must not be before 'arrivalDate'"))
    }
}
