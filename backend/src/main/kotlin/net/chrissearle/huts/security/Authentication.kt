package net.chrissearle.huts.security

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.jwt.jwt
import java.net.URI
import java.util.concurrent.TimeUnit

const val AUTH_PROVIDER_NAME = "hytter"

private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_HOURS = 24L
private const val JWK_RATE_LIMIT_REQUESTS = 10L
private const val JWK_RATE_LIMIT_MINUTES = 1L

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

private fun AuthenticationConfig.keycloakJwt(
    name: String?,
    config: AuthConfig,
) {
    val issuer = requireNotNull(config.issuer) { "KEYCLOAK_ISSUER not set" }
    val clientId = requireNotNull(config.clientId) { "KEYCLOAK_CLIENT_ID not set" }

    val jwkProvider =
        JwkProviderBuilder(URI("$issuer/protocol/openid-connect/certs").toURL())
            .cached(JWK_CACHE_SIZE, JWK_CACHE_HOURS, TimeUnit.HOURS)
            .rateLimited(JWK_RATE_LIMIT_REQUESTS, JWK_RATE_LIMIT_MINUTES, TimeUnit.MINUTES)
            .build()

    jwt(name) {
        verifier(jwkProvider, issuer)
        validate { credential ->
            val clientRoles =
                credential.payload
                    .getClaim("resource_access")
                    ?.asMap()
                    ?.get(clientId)
                    ?.let { it as? Map<*, *> }
                    ?.get("roles")
                    ?.let { it as? List<*> }
                    ?.filterIsInstance<String>()
                    ?.toSet()
                    .orEmpty()
            val displayName = credential.payload.getClaim("name")?.asString() ?: return@validate null

            HytterPrincipal(name = displayName, roles = clientRoles)
        }
    }
}

fun AuthenticationConfig.configureHytterAuth(config: AuthConfig) {
    if (config.disabled) {
        dev(AUTH_PROVIDER_NAME)
    } else {
        keycloakJwt(AUTH_PROVIDER_NAME, config)
    }
}
