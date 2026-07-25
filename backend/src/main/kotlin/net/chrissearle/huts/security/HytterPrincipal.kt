package net.chrissearle.huts.security

data class HytterPrincipal(
    /** The Keycloak `sub` claim - stable across name changes, unlike [name]. */
    val subject: String,
    val name: String,
    val roles: Set<String>,
) {
    val isAdmin: Boolean get() = "admin" in roles
}
