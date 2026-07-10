package net.chrissearle.huts.routes

import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import net.chrissearle.huts.security.OAUTH_PROVIDER_NAME
import net.chrissearle.huts.security.UserSession
import net.chrissearle.huts.security.clientRoles
import net.chrissearle.huts.security.decodeAccessTokenClaims
import net.chrissearle.huts.security.displayName

fun Route.authRoutes(
    publicUrl: String,
    issuer: String,
    clientId: String,
) {
    authenticate(OAUTH_PROVIDER_NAME) {
        get("/login") {
            // Never reached: the oauth provider intercepts and redirects to Keycloak.
        }

        get("/callback") {
            val principal =
                call.principal<OAuthAccessTokenResponse.OAuth2>()
                    ?: return@get call.respondRedirect("/login")

            val claims = decodeAccessTokenClaims(principal.accessToken)
            val name = claims.displayName() ?: "Ukjent bruker"
            val roles = claims.clientRoles(clientId)

            call.sessions.set(UserSession(name = name, roles = roles))
            call.respondRedirect(publicUrl)
        }
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        val endSessionUrl =
            "$issuer/protocol/openid-connect/logout" +
                "?client_id=$clientId&post_logout_redirect_uri=$publicUrl"
        call.respondRedirect(endSessionUrl)
    }
}
