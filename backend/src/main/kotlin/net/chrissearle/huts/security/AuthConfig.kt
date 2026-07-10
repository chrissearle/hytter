package net.chrissearle.huts.security

data class AuthConfig(
    val disabled: Boolean,
    val issuer: String?,
    val clientId: String?,
) {
    companion object {
        fun fromEnv(): AuthConfig {
            val disabled = System.getenv("AUTH_DISABLED")?.toBoolean() ?: false
            return AuthConfig(
                disabled = disabled,
                issuer = System.getenv("KEYCLOAK_ISSUER"),
                clientId = System.getenv("KEYCLOAK_CLIENT_ID"),
            )
        }
    }
}
