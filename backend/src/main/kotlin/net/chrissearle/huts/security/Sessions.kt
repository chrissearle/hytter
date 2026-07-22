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

private fun envKeyOrRandom(
    name: String,
    length: Int,
): ByteArray {
    val fromEnv = System.getenv(name)?.takeIf { it.isNotBlank() }
    if (fromEnv != null) return fromEnv.hexToByteArray()
    return ByteArray(length).also { SecureRandom().nextBytes(it) }
}

fun Application.configureSessions() {
    // A random key is fine for AUTH_DISABLED dev usage; set SESSION_ENCRYPT_KEY /
    // SESSION_SIGN_KEY (hex-encoded) in real deployments so sessions survive restarts
    // and are shared across replicas.
    val encryptKey = envKeyOrRandom("SESSION_ENCRYPT_KEY", SESSION_KEY_LENGTH)
    val signKey = envKeyOrRandom("SESSION_SIGN_KEY", SESSION_KEY_LENGTH)

    install(Sessions) {
        cookie<UserSession>(SESSION_COOKIE_NAME) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "Lax"
            serializer = KotlinxSessionSerializer(Json)
            transform(SessionTransportTransformerEncrypt(encryptKey, signKey))
        }
    }
}
