package net.chrissearle.huts.security

import kotlinx.serialization.Serializable

/**
 * What the encrypted `HYTTER_SESSION` cookie holds. The access token is
 * deliberately *not* stored: the backend has no use for it once the claims are
 * decoded, and leaving it out keeps the cookie well clear of the browser's 4KB
 * limit. The refresh token is kept so [TokenRefresher] can mint a new access
 * token, re-deriving [name] and [roles] each time - which is why a role change
 * in Keycloak takes effect within one access-token lifetime rather than never.
 *
 * [refreshExpiresAt] is the hard ceiling on a login. It always comes from
 * Keycloak's `refresh_expires_in` and is never extended locally, so a session
 * can never outlive what Keycloak granted.
 */
@Serializable
data class UserSession(
    val subject: String,
    val name: String,
    val roles: Set<String>,
    /** Epoch seconds at which the current access token expires. */
    val expiresAt: Long,
    val refreshToken: String,
    /** Epoch seconds at which the refresh token expires - the end of the login. */
    val refreshExpiresAt: Long,
)
