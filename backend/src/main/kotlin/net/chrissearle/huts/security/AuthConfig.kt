package net.chrissearle.huts.security

data class AuthConfig(
    val disabled: Boolean,
    val issuer: String?,
    val clientId: String?,
    val clientSecret: String?,
    // Externally reachable origin of the frontend, e.g. https://hytter.example.com.
    // Keycloak redirects the browser here after login, so it must match the
    // frontend's public ingress host, not the backend's in-cluster address.
    val publicUrl: String,
) {
    companion object {
        fun fromEnv(): AuthConfig {
            val disabled = System.getenv("AUTH_DISABLED")?.toBoolean() ?: false
            return AuthConfig(
                disabled = disabled,
                issuer = System.getenv("KEYCLOAK_ISSUER"),
                clientId = System.getenv("KEYCLOAK_CLIENT_ID"),
                clientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET"),
                publicUrl = System.getenv("PUBLIC_URL") ?: "http://localhost:3000",
            )
        }
    }
}
