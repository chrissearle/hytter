package net.chrissearle.huts.security

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.response.respond
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

const val AUTH_PROVIDER_NAME = "hytter"
const val OAUTH_PROVIDER_NAME = "hytter-oauth"
const val CALLBACK_PATH = "/callback"

private val devPrincipal =
    HytterPrincipal(subject = "dev-admin", name = "Admin", roles = setOf("admin", "user", "group-ha12"))

private class DevAuthenticationProvider(
    config: Config,
) : AuthenticationProvider(config) {
    class Config(
        name: String?,
    ) : AuthenticationProvider.Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(name, devPrincipal)
    }
}

private fun AuthenticationConfig.dev(name: String?) {
    register(DevAuthenticationProvider(DevAuthenticationProvider.Config(name)))
}

private fun UserSession.toPrincipal() = HytterPrincipal(subject = subject, name = name, roles = roles)

private const val SESSION_CHALLENGE_KEY = "HytterSessionAuth"

/**
 * Every authenticated request runs the session through [TokenRefresher], so a
 * login ends exactly when Keycloak says it does and role changes propagate
 * within one access-token lifetime.
 *
 * Deliberately hand-rolled rather than Ktor's `session<UserSession>` provider.
 * That one picks its failure cause from cookie *presence* alone, so an expired
 * session - cookie there, but rejected - reports `InvalidCredentials`, which
 * makes Ktor run the challenge even inside `authenticate(optional = true)` and
 * 401s the public calendar. See `SessionAuth.kt` / `AuthenticationInterceptors.kt`.
 */
private class KeycloakSessionProvider(
    config: Config,
    private val refresher: TokenRefresher,
) : AuthenticationProvider(config) {
    class Config(
        name: String?,
    ) : AuthenticationProvider.Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call

        val principal =
            call.sessions.get<UserSession>()?.let { session ->
                when (val outcome = refresher.ensureFresh(session)) {
                    is SessionOutcome.Valid -> {
                        outcome.session.toPrincipal()
                    }

                    is SessionOutcome.Refreshed -> {
                        call.sessions.set(outcome.session)
                        outcome.session.toPrincipal()
                    }

                    SessionOutcome.Expired -> {
                        call.sessions.clear<UserSession>()
                        null
                    }
                }
            }

        if (principal != null) {
            context.principal(name, principal)
            return
        }

        // Always NoCredentials, never InvalidCredentials: an expired session has
        // just been cleared, so this caller genuinely is anonymous from here on.
        // That lets optional routes carry on serving them, while a required
        // `authenticate` block still challenges.
        context.challenge(SESSION_CHALLENGE_KEY, AuthenticationFailedCause.NoCredentials) { challenge, challengeCall ->
            challengeCall.respond(HttpStatusCode.Unauthorized)
            challenge.complete()
        }
    }
}

internal fun AuthenticationConfig.keycloakSession(
    name: String?,
    refresher: TokenRefresher,
) {
    register(KeycloakSessionProvider(KeycloakSessionProvider.Config(name), refresher))
}

private fun AuthenticationConfig.keycloakOAuth(
    name: String?,
    config: AuthConfig,
    httpClient: HttpClient,
) {
    val issuer = requireNotNull(config.issuer) { "KEYCLOAK_ISSUER not set" }
    val clientId = requireNotNull(config.clientId) { "KEYCLOAK_CLIENT_ID not set" }
    val clientSecret = requireNotNull(config.clientSecret) { "KEYCLOAK_CLIENT_SECRET not set" }

    oauth(name) {
        client = httpClient
        // Must match the frontend's public callback URL exactly: Keycloak redirects the
        // browser here directly, so it can never be the in-cluster backend address.
        urlProvider = { "${config.publicUrl}$CALLBACK_PATH" }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "keycloak",
                authorizeUrl = "$issuer/protocol/openid-connect/auth",
                accessTokenUrl = tokenEndpoint(issuer),
                requestMethod = HttpMethod.Post,
                clientId = clientId,
                clientSecret = clientSecret,
                // Never add `offline_access`: that yields an offline token which
                // effectively never expires, defeating the session lifetime bound.
                defaultScopes = listOf("openid", "profile"),
            )
        }
    }
}

fun AuthenticationConfig.configureHytterAuth(
    config: AuthConfig,
    httpClient: HttpClient,
    refresher: TokenRefresher?,
) {
    if (config.disabled) {
        dev(AUTH_PROVIDER_NAME)
    } else {
        keycloakSession(AUTH_PROVIDER_NAME, requireNotNull(refresher) { "TokenRefresher required" })
        keycloakOAuth(OAUTH_PROVIDER_NAME, config, httpClient)
    }
}
