package net.chrissearle.huts.security

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import io.ktor.server.sessions.clear
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

/**
 * Every authenticated request runs the session through [TokenRefresher], so a
 * login ends exactly when Keycloak says it does and role changes propagate
 * within one access-token lifetime.
 */
private fun AuthenticationConfig.keycloakSession(
    name: String?,
    refresher: TokenRefresher,
) {
    session<UserSession>(name) {
        validate { session ->
            when (val outcome = refresher.ensureFresh(session)) {
                is SessionOutcome.Valid -> {
                    outcome.session.toPrincipal()
                }

                is SessionOutcome.Refreshed -> {
                    sessions.set(outcome.session)
                    outcome.session.toPrincipal()
                }

                SessionOutcome.Expired -> {
                    sessions.clear<UserSession>()
                    null
                }
            }
        }
        challenge { call.respond(HttpStatusCode.Unauthorized) }
    }
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
