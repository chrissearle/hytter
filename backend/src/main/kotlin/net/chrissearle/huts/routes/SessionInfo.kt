package net.chrissearle.huts.routes

import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import net.chrissearle.huts.security.HytterPrincipal

@Serializable
data class SessionInfo(
    val authenticated: Boolean,
    val name: String?,
    val isAdmin: Boolean,
    /**
     * Logged in but without a `hytter` client role: the realm is shared, so this
     * is a real state the GUI has to explain rather than silently 403.
     */
    val hasAccess: Boolean,
)

fun Route.sessionRoute() {
    get("/api/session") {
        val principal = call.principal<HytterPrincipal>()
        call.respond(
            SessionInfo(
                authenticated = principal != null,
                name = principal?.name,
                isAdmin = principal?.isAdmin ?: false,
                hasAccess = principal?.hasAccess ?: false,
            ),
        )
    }
}
