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

fun ApiError.messageMap(): Map<String, ErrorResponse> =
    when (this) {
        is UpstreamError -> mapOf("upstream" to upstream, "error" to response)
        else -> mapOf("error" to response)
    }

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

data object HutRequired : RequiredField(fieldName = "hutId")

data object NotBookingOwner : ApiError {
    override val response =
        ErrorResponse(status = HttpStatusCode.Forbidden, message = "You may only edit your own bookings")
}

data class VersionNotReadable(
    val e: Throwable,
) : ApiError {
    override val response = ErrorResponse(status = HttpStatusCode.InternalServerError, message = "${e.message}")
}
