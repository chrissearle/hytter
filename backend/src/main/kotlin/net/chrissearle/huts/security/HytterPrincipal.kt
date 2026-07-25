package net.chrissearle.huts.security

const val ROLE_ADMIN = "admin"
const val ROLE_USER = "user"

data class HytterPrincipal(
    /** The Keycloak `sub` claim - stable across name changes, unlike [name]. */
    val subject: String,
    val name: String,
    val roles: Set<String>,
) {
    val isAdmin: Boolean get() = ROLE_ADMIN in roles

    /**
     * Whether this account is entitled to the app at all. The Keycloak realm is
     * shared with other sites, so merely holding an account there is not enough -
     * a `hytter` client role has to have been granted. Admin implies access.
     */
    val hasAccess: Boolean get() = isAdmin || ROLE_USER in roles
}
