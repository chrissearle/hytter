package net.chrissearle.huts.routes

import io.ktor.http.URLBuilder
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
import net.chrissearle.huts.security.TokenResponse
import net.chrissearle.huts.security.UserSession
import net.chrissearle.huts.security.toSession

private const val MILLIS_PER_SECOND = 1000L

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

            call.sessions.set(principal.toUserSession(clientId))
            call.respondRedirect(publicUrl)
        }
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect(logoutUrl(issuer, clientId, publicUrl))
    }
}

/**
 * Keycloak returns `refresh_expires_in` alongside the standard fields; Ktor
 * surfaces anything it does not model itself in [OAuthAccessTokenResponse.OAuth2.extraParameters].
 */
private fun OAuthAccessTokenResponse.OAuth2.toUserSession(clientId: String): UserSession =
    TokenResponse(
        accessToken = accessToken,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        refreshExpiresIn = extraParameters["refresh_expires_in"]?.toLongOrNull(),
    ).toSession(clientId = clientId, timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND)

fun logoutUrl(
    issuer: String,
    clientId: String,
    publicUrl: String,
): String =
    URLBuilder("$issuer/protocol/openid-connect/logout")
        .apply {
            parameters.append("client_id", clientId)
            parameters.append("post_logout_redirect_uri", publicUrl)
        }.buildString()
