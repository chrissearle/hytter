package net.chrissearle.huts.security

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.auth.session
import io.ktor.server.response.respond

const val AUTH_PROVIDER_NAME = "hytter"
const val OAUTH_PROVIDER_NAME = "hytter-oauth"
const val CALLBACK_PATH = "/callback"

private val devPrincipal = HytterPrincipal(name = "Admin", roles = setOf("admin", "user"))

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

private fun AuthenticationConfig.keycloakSession(name: String?) {
    session<UserSession>(name) {
        validate { session -> HytterPrincipal(name = session.name, roles = session.roles) }
        challenge { call.respond(io.ktor.http.HttpStatusCode.Unauthorized) }
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
                accessTokenUrl = "$issuer/protocol/openid-connect/token",
                requestMethod = HttpMethod.Post,
                clientId = clientId,
                clientSecret = clientSecret,
                defaultScopes = listOf("openid", "profile"),
            )
        }
    }
}

fun AuthenticationConfig.configureHytterAuth(
    config: AuthConfig,
    httpClient: HttpClient,
) {
    if (config.disabled) {
        dev(AUTH_PROVIDER_NAME)
    } else {
        keycloakSession(AUTH_PROVIDER_NAME)
        keycloakOAuth(OAUTH_PROVIDER_NAME, config, httpClient)
    }
}
