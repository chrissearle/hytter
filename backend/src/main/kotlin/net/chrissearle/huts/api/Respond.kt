package net.chrissearle.huts.api

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

context(context: RoutingContext)
suspend inline fun <reified A : Any> Either<ApiError, A>.respond(status: HttpStatusCode = HttpStatusCode.OK) {
    onLeft { context.respond(it) }
    onRight { context.call.respond(status, it) }
}

suspend fun RoutingContext.respond(error: ApiError) = call.respond(error.status(), error.messageMap())
