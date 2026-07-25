package net.chrissearle.huts.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

/**
 * Anything that escapes a route's `either { }` block still leaves as the same
 * JSON envelope every handled error uses, rather than Ktor's default plain-text
 * page. As with [messageMap], the cause stays in the log - the client is told
 * only that something failed.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        // Raised by call.receive when the body is missing or not valid JSON.
        exception<BadRequestException> { call, cause ->
            logger.info(cause) { "Malformed request" }
            call.respondError(HttpStatusCode.BadRequest, "Malformed request")
        }

        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception" }
            call.respondError(HttpStatusCode.InternalServerError, "Internal server error")
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String,
) = respond(status, mapOf("error" to ErrorResponse(status = status, message = message)))
