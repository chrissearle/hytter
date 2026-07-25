package net.chrissearle.huts.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.serialization.KotlinxSessionSerializer
import kotlinx.serialization.json.Json
import java.security.SecureRandom

const val SESSION_COOKIE_NAME = "HYTTER_SESSION"
private const val SESSION_KEY_LENGTH = 16

/**
 * Upper bound on how long the browser keeps the cookie. This is only defence in
 * depth: the authoritative check is `UserSession.refreshExpiresAt`, which comes
 * from Keycloak and is enforced on every request in [TokenRefresher.ensureFresh].
 * Keep it at or above Keycloak's SSO Session Max so the cookie never outlives
 * the grant, and never the other way round.
 */
private const val DEFAULT_SESSION_MAX_AGE_SECONDS = 36000L

private fun envKeyOrRandom(
    name: String,
    length: Int,
): ByteArray {
    val fromEnv = System.getenv(name)?.takeIf { it.isNotBlank() }
    if (fromEnv != null) return fromEnv.hexToByteArray()
    return ByteArray(length).also { SecureRandom().nextBytes(it) }
}

fun Application.configureSessions(config: AuthConfig) {
    // A random key is fine for AUTH_DISABLED dev usage; set SESSION_ENCRYPT_KEY /
    // SESSION_SIGN_KEY (hex-encoded) in real deployments so sessions survive restarts
    // and are shared across replicas.
    val encryptKey = envKeyOrRandom("SESSION_ENCRYPT_KEY", SESSION_KEY_LENGTH)
    val signKey = envKeyOrRandom("SESSION_SIGN_KEY", SESSION_KEY_LENGTH)
    val maxAge =
        System.getenv("SESSION_MAX_AGE_SECONDS")?.toLongOrNull() ?: DEFAULT_SESSION_MAX_AGE_SECONDS

    install(Sessions) {
        cookie<UserSession>(SESSION_COOKIE_NAME) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = config.secureCookie
            cookie.maxAgeInSeconds = maxAge
            // Lax rather than Strict: Keycloak sends the browser back to /callback
            // as a cross-site top-level GET, which Strict would strip the cookie from.
            cookie.extensions["SameSite"] = "Lax"
            serializer = KotlinxSessionSerializer(Json)
            transform(SessionTransportTransformerEncrypt(encryptKey, signKey))
        }
    }
}
