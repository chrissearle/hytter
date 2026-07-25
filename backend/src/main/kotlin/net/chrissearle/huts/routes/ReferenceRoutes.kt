package net.chrissearle.huts.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.chrissearle.huts.domain.Reference

/**
 * Dropdown contents for the booking form. Public: the form itself is open to
 * anonymous visitors, and this is static reference data.
 */
fun Route.referenceRoutes() {
    get("/api/reference") {
        call.respond(Reference.current)
    }
}
