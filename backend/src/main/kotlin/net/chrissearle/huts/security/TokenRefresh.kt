package net.chrissearle.huts.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/** Refresh this many seconds before the access token actually expires. */
private const val EXPIRY_SKEW_SECONDS = 30L

/**
 * Fallback when Keycloak omits `refresh_expires_in`. Deliberately short: an
 * unknown ceiling must not become an unbounded login.
 */
private const val FALLBACK_REFRESH_LIFETIME_SECONDS = 1800L

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long? = null,
)

/** What [TokenRefresher.ensureFresh] concluded about a session. */
sealed interface SessionOutcome {
    /** Still valid as-is; nothing to write back. */
    data class Valid(
        val session: UserSession,
    ) : SessionOutcome

    /** Renewed against Keycloak; the caller must persist [session] to the cookie. */
    data class Refreshed(
        val session: UserSession,
    ) : SessionOutcome

    /** Over, for good. The caller must clear the cookie. */
    data object Expired : SessionOutcome
}

class TokenRefresher(
    private val issuer: String,
    private val clientId: String,
    private val clientSecret: String,
    private val httpClient: HttpClient,
    private val now: () -> Long = { System.currentTimeMillis() / MILLIS_PER_SECOND },
) {
    /**
     * Enforces the session lifetime rule on every request:
     *
     * 1. Past the refresh token's expiry, the login is over - no amount of
     *    activity extends it, because that ceiling came from Keycloak.
     * 2. Inside the access token's lifetime, use the session as-is.
     * 3. Otherwise renew, which re-reads name and roles from the fresh token.
     */
    suspend fun ensureFresh(session: UserSession): SessionOutcome {
        val timestamp = now()

        return when {
            timestamp >= session.refreshExpiresAt -> {
                logger.debug { "Refresh token expired for ${session.subject}; ending session" }
                SessionOutcome.Expired
            }

            timestamp < session.expiresAt - EXPIRY_SKEW_SECONDS -> {
                SessionOutcome.Valid(session)
            }

            else -> {
                refresh(session.refreshToken, timestamp)
                    ?.let { SessionOutcome.Refreshed(it) }
                    ?: SessionOutcome.Expired
            }
        }
    }

    private suspend fun refresh(
        refreshToken: String,
        timestamp: Long,
    ): UserSession? {
        val response: HttpResponse? =
            runCatching {
                httpClient.submitForm(
                    url = tokenEndpoint(issuer),
                    formParameters =
                        Parameters.build {
                            append("grant_type", "refresh_token")
                            append("client_id", clientId)
                            append("client_secret", clientSecret)
                            append("refresh_token", refreshToken)
                        },
                )
            }.onFailure { error ->
                logger.warn(error) { "Token refresh call to Keycloak failed" }
            }.getOrNull()

        return when {
            response == null -> {
                null
            }

            // A revoked, reused or simply expired refresh token lands here.
            !response.status.isSuccess() -> {
                logger.info { "Token refresh rejected by Keycloak: ${response.status}" }
                null
            }

            else -> {
                runCatching {
                    response
                        .body<TokenResponse>()
                        .toSession(clientId, timestamp, previousRefreshToken = refreshToken)
                }.onFailure { error ->
                    logger.warn(error) { "Could not read the refreshed token" }
                }.getOrNull()
            }
        }
    }
}

fun tokenEndpoint(issuer: String) = "$issuer/protocol/openid-connect/token"

private const val MILLIS_PER_SECOND = 1000L

/**
 * Builds the session from a token response. [refreshExpiresAt] is taken
 * verbatim from Keycloak - on every refresh Keycloak caps it by the remaining
 * SSO Session Max, so it walks down to zero and the login ends on schedule.
 */
fun TokenResponse.toSession(
    clientId: String,
    timestamp: Long,
    previousRefreshToken: String? = null,
): UserSession {
    val claims = decodeAccessTokenClaims(accessToken)
    return UserSession(
        subject = claims.subject() ?: "unknown",
        name = claims.displayName() ?: "Ukjent bruker",
        roles = claims.clientRoles(clientId),
        expiresAt = timestamp + expiresIn,
        refreshToken = refreshToken ?: previousRefreshToken.orEmpty(),
        refreshExpiresAt = timestamp + (refreshExpiresIn ?: FALLBACK_REFRESH_LIFETIME_SECONDS),
    )
}
