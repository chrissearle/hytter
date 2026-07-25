package net.chrissearle.huts.api

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    @Serializable(with = HttpStatusCodeSerializer::class)
    val status: HttpStatusCode,
    val message: String,
    val fieldValue: String? = null,
)

sealed interface ApiError {
    val response: ErrorResponse
}

fun ApiError.status() = response.status

/**
 * Only [response] is ever serialized. An [UpstreamError]'s `upstream` detail -
 * a raw Postgres constraint message, say - stays server-side for the logs: it
 * describes our internals and has no business reaching a client.
 */
fun ApiError.messageMap(): Map<String, ErrorResponse> = mapOf("error" to response)

abstract class UpstreamError(
    open val upstream: ErrorResponse,
    val systemName: String,
) : ApiError {
    override val response =
        ErrorResponse(
            status = HttpStatusCode.InternalServerError,
            message = "call to $systemName failed",
        )
}

abstract class RequiredField(
    val fieldName: String,
) : ApiError {
    override val response =
        ErrorResponse(
            status = HttpStatusCode.BadRequest,
            message = "$fieldName required",
        )
}

data class DatabaseCallFailed(
    override val upstream: ErrorResponse,
) : UpstreamError(upstream = upstream, systemName = "Database")

data object PrincipalMissing : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.Unauthorized, message = "Principal missing")
}

data class BookingNotFound(
    val id: Int,
) : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.NotFound, message = "Booking not found: $id")
}

/** A malformed id in the path - a bad request, not a missing booking. */
data class InvalidBookingId(
    val value: String?,
) : ApiError {
    override val response =
        ErrorResponse(status = HttpStatusCode.BadRequest, message = "Invalid booking id", fieldValue = value)
}

data class InvalidDateRange(
    val reason: String,
) : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.BadRequest, message = reason)
}

data object NameRequired : RequiredField(fieldName = "name")

data object InvalidNumberOfPeople : ApiError {
    override val response =
        ErrorResponse(status = HttpStatusCode.BadRequest, message = "numberOfPeople must be greater than 0")
}

data class AdminNotesTooLong(
    val maxLength: Int,
) : ApiError {
    override val response =
        ErrorResponse(
            status = HttpStatusCode.BadRequest,
            message = "adminNotes must be at most $maxLength characters",
        )
}

data object NotBookingOwner : ApiError {
    override val response =
        ErrorResponse(status = HttpStatusCode.Forbidden, message = "You may only edit your own bookings")
}

data object AdminRequired : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.Forbidden, message = "Admin role required")
}

data class VersionNotReadable(
    val e: Throwable,
) : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.InternalServerError, message = "${e.message}")
}
